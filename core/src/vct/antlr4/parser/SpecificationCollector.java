package vct.antlr4.parser;

import hre.ast.FileOrigin;
import hre.io.FifoStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import vct.col.ast.*;
import vct.col.rewrite.AbstractRewriter;
import vct.parsers.CMLLexer;
import vct.parsers.CMLParser;
import static hre.System.*;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Lexer;

/**
 * Rewrite an AST with specifications in the form of comments
 * to an AST with specifications in the from of ASTs. 
 */
public class SpecificationCollector extends AbstractRewriter {
  
  public SpecificationCollector(ProgramUnit source) {
    super(source);
    currentContractBuilder=new ContractBuilder();
  }

  
  
  @Override
  public void visit(ASTSpecialDeclaration s){
    switch(s.kind){
    case Given:
    case Yields:
    case Requires:
    case Ensures:
    case Invariant:
    case Modifies:
    case Accessible:
      if (currentContractBuilder != null) break;
    default:
      super.visit(s);
      return;
    }
    switch(s.kind){
    case Modifies:{
      currentContractBuilder.modifies(rewrite(s.args));
      break;
    }
    case Accessible:{
      currentContractBuilder.accesses(rewrite(s.args));
      break;
    }
    case Given:
    {
      BlockStatement block=(BlockStatement)s.args[0];
      int N=block.getLength();
      for(int i=0;i<N;i++){
        currentContractBuilder.given((DeclarationStatement)rewrite(block.get(i)));
      }
      break;
    }
    case Yields:
    {
      BlockStatement block=(BlockStatement)s.args[0];
      int N=block.getLength();
      for(int i=0;i<N;i++){
        currentContractBuilder.yields((DeclarationStatement)rewrite(block.get(i)));
      }
      break;
    }
    case Requires:
      currentContractBuilder.requires(rewrite(s.args[0]));
      break;
    case Ensures:
      currentContractBuilder.ensures(rewrite(s.args[0]));
      break;
    case Invariant:
      currentContractBuilder.appendInvariant(rewrite(s.args[0]));
      break;
    default:
      Abort("Missing case %s",s.kind);
    }
  }

  @Override
  public void visit(ASTClass c){

    String name=c.getName();
    if (name==null) {
      Abort("illegal class without name");
    } else {
      Debug("rewriting class "+name);
      ASTClass res=new ASTClass(name,c.kind,rewrite(c.parameters),rewrite(c.super_classes),rewrite(c.implemented_classes));
      res.setOrigin(c.getOrigin());
      currentTargetClass=res;
      Contract contract=c.getContract();
      if (currentContractBuilder==null){
        currentContractBuilder=new ContractBuilder();
      }
      if (contract!=null){
        rewrite(contract,currentContractBuilder);
      }
      res.setContract(currentContractBuilder.getContract());
      currentContractBuilder=new ContractBuilder();
      for(ASTNode item:c){
        res.add(rewrite(item));
      }
      result=res;
      currentTargetClass=null;
    }

    if (!currentContractBuilder.isEmpty()){
      Abort("class contains unattached contract clauses");
    }
    currentContractBuilder=null;
  }

  @Override
  public void visit(Method m){
    super.visit(m);
    currentContractBuilder=new ContractBuilder();
  }
  @Override
  public void visit(LoopStatement s){
    BlockStatement new_before=create.block();
    BlockStatement new_after=create.block();
    BlockStatement old_before=s.get_before();
    BlockStatement old_after=s.get_after();
    
    LoopStatement res=new LoopStatement();
    ASTNode tmp;
    tmp=s.getInitBlock();
    if (tmp!=null) res.setInitBlock(tmp.apply(this));
    tmp=s.getUpdateBlock();
    if (tmp!=null) res.setUpdateBlock(tmp.apply(this));
    tmp=s.getEntryGuard();
    if (tmp!=null) res.setEntryGuard(tmp.apply(this));
    tmp=s.getExitGuard();
    if (tmp!=null) res.setExitGuard(tmp.apply(this));
    if (currentContractBuilder==null) currentContractBuilder=new ContractBuilder();
    rewrite(s.getContract(),currentContractBuilder);
    filter_with_then(new_before, new_after, new_before, old_before);
    filter_with_then(new_before, new_after, new_after, old_after);
    res.setContract(currentContractBuilder.getContract(false));
    currentContractBuilder=null;
    res.set_before(new_before);
    res.set_after(new_after);
    res.setBody(rewrite(s.getBody()));
    res.setOrigin(s);
    result=res;
  }

  private void filter_with_then(BlockStatement new_before,
      BlockStatement new_after, BlockStatement new_current, BlockStatement old_current) {
    for(ASTNode n:old_current){
      if (n instanceof ASTSpecialDeclaration){
        ASTSpecialDeclaration sp=(ASTSpecialDeclaration)n; 
        switch(sp.kind){
        case Requires:{
          currentContractBuilder.requires(copy_rw.rewrite(sp.args[0]));
          continue;
        }
        case Ensures:{
          currentContractBuilder.ensures(copy_rw.rewrite(sp.args[0]));
          continue;
        }
        }
      } else if (n instanceof ASTSpecial){
        ASTSpecial sp=(ASTSpecial)n; 
        switch(sp.kind){
        case With:{
          for(ASTNode s:(BlockStatement)sp.args[0]){
            new_before.add_statement(copy_rw.rewrite(s));
          }
          continue;
        }
        case Then:{
          for(ASTNode s:(BlockStatement)sp.args[0]){
            new_after.add_statement(copy_rw.rewrite(s));
          }
          continue;
        }
        }
      }
      new_current.add_statement(copy_rw.rewrite(n));
    }
  }
  
  @Override
  public void visit(BlockStatement block){
    BlockStatement tmp=currentBlock;
    currentBlock=create.block();
    
    int N=block.getLength();
    for(int i=0;i<N;i++){
      if (block.get(i) instanceof ASTSpecialDeclaration && currentContractBuilder==null){
        int j;
        for(j=i+1;j<N && (block.get(j) instanceof ASTSpecialDeclaration);j++){
        }
        if (j<N && block.get(j) instanceof LoopStatement) {
          currentContractBuilder=new ContractBuilder();
        } else {
          j--;
          for(;i<j;i++){
            currentBlock.add(rewrite(block.get(i)));
          }
        }
      }
      currentBlock.add(rewrite(block.get(i)));
      //if (block.get(i) instanceof LoopStatement){
      //  
      //}
    }
    
    result=currentBlock;
    currentBlock=tmp;
  }
}

