/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mods.basemod.interfaces;

import java.util.Map;
import utils.containers.id.Mid;
import utils.containers.json.JSONObject;

/**
 *
 * @author ilya
 */
public interface InvItem extends Base {

 public Object getParam ( String k );

 public String getAllP ();

 public void addParam ( String k, String v );

 public void addAllP ( Map<String, String> p );

 public void toJSON ( JSONObject o );

 @Override
 public Mid getId ();

}
