/*    */ package org.jnbt;
/*    */ 
/*    */ public abstract class Tag
/*    */ {
/*    */   private final String name;
/*    */ 
/*    */   public Tag(String name)
/*    */   {
/* 53 */     this.name = name;
/*    */   }
/*    */ 
/*    */   public final String getName()
/*    */   {
/* 61 */     return this.name;
/*    */   }
/*    */ 
/*    */   public abstract Object getValue();
/*    */ }

/* Location:           C:\Users\Fernando\.m2\repository\jnbt\jnbt\1.1\jnbt-1.1.jar
 * Qualified Name:     org.jnbt.Tag
 * JD-Core Version:    0.6.2
 */