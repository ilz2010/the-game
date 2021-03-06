package mods.basemod;

import java.io.Serializable;
import java.util.*;
import mods.basemod.containers.Server;
import mods.basemod.interfaces.InvItem;
import mods.basemod.resources.Model;
import utils.containers.id.Mid;
import utils.containers.json.JSONObject;

public class IItem implements Serializable, InvItem {

 protected final Mid id;
 protected final TreeMap<String, Object> param;

 public TreeMap<String, Object> getParam () {
  return param;
 }
 protected final Model model;

 public IItem ( Mid id, Model model, Map<String, String> map ) {
  if ( map == null ) {
   param = null;
  } else {
   this.param = new TreeMap<>(map);
  }
  this.id = id;
  this.model = model;
 }

 public IItem ( String m, JSONObject o ) {
  this.id = Server.instanceMid(m, o.getString("Iid"), o.getString("Sid"));
  this.model = Server.instanceModel(Server.instanceRid(id, Resource.Type.Model, "model"), o.getString("Model"));
  this.param = new TreeMap<>(o.getJSONObject("Params").getMap());
 }

 @Override
 public Object getParam ( String k ) {
  Object t = "";
  try {
   t = param.get(k);
  } catch ( Exception e ) {
   main.Main.LOG.addE(e);
  }
  return t;
 }

 @Override
 public String getAllP () {
  String t = "";
  t = param.keySet().stream().
     map(( s ) -> s + "=" + param.get(s) + "; ").
     reduce(t, String::concat);
  return t;
 }

 @Override
 public void addParam ( String k, String v ) {
  this.param.put(k, v);
 }

 @Override
 public void addAllP ( Map<String, String> p ) {
  this.param.putAll(p);
 }

 @Override
 public Mid getId () {
  return id;
 }

 public Model getModel () {
  return model;
 }

 @Override
 public String toString () {
  return "IItem, " + id.toString() + " " + getAllP();
 }

 @Override
 public void toJSON ( JSONObject o ) {
  o.put("Iid", id.getIid());
  o.put("Sid", id.getSid());
  o.put("Model", model.getFile());
  o.put("Params", param);
 }
}
