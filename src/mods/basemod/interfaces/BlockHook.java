package mods.basemod.interfaces;

import utils.containers.id.Mid;

public interface BlockHook {

 public void blockClicked ( Mid item );

 public void blockDestroyed ( Mid item );

 public void blockPlaced ();

}
