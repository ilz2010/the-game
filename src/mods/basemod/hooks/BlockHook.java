package mods.basemod.hooks;

import mods.basemod.containers.Mid;

public interface BlockHook {
 public void blockClicked(Mid item);
 public void blockDestroyed(Mid item);
 public void blockPlaced();
 
}