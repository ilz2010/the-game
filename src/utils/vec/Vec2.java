package utils.vec;
public class Vec2 <T>{
 private T x;
 private T y;
 
 public Vec2(){x = null; y = null;}
 public Vec2(T x, T y){}
 
 public void sX(T x){ this.x = x; }
 public void sY(T y){ this.y = y; }
 
 public T gX(){ return x; }
 public T gY(){ return y;}
 
}