package mods.basemod;

import java.io.Serializable;
import static main.Main.LOG;
import mods.basemod.containers.Server;
import utils.containers.id.Rid;

public abstract class Resource implements Serializable {

 public enum Type {

  Model, Sound, Null
 }
 protected final String url;
 protected final Rid id;
 protected final Type type;

 public Resource ( Rid id, Type type, String url ) {
  this.id = id;
  this.url = url;
  this.type = type;
 }

 public String getUrl () {
  return url;
 }

 public Rid getId () {
  return id;
 }

 public Type getType () {
  return type;
 }

 public static Resource getResource ( Rid k, String v ) {
  switch ( k.getType() ) {
   case Model:
    return Server.instanceModel(k, v);
   case Sound:
    return Server.instanceSound(k, v);
   default:
    LOG.addE("Error with parse Resource type " + k.toString());
    return null;

  }
 }

 public static Type getType ( String s ) {
  switch ( s ) {
   case "Model":
    return Type.Model;
   case "Sound":
    return Type.Sound;
   default:
    LOG.addE("Error with parse type " + s);
    return Type.Null;
  }
 }
}
