package render.gui.widgets;

import java.awt.*;
import java.io.*;
import main.Main;
import org.fenggui.appearance.TextAppearance;
import org.fenggui.binding.render.text.ITextRenderer;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.text.content.factory.simple.TextStyle;
import org.fenggui.util.*;
import org.fenggui.util.Color;
import org.fenggui.util.fonttoolkit.FontFactory;

public class StdButton extends StdWidget {

 public StdButton ( String text, IButtonPressedListener a ) {
  super("res/mbut.png", "res/mbutf.png", "res/mbutn.png");
  setText(text);
  this.setEnabled(false);
  setFont();
  addButtonPressedListener(a);
  setEnabled(true);
 }

 public StdButton ( String text ) {
  super("res/mbut.png", "res/mbutf.png", "res/mbutn.png");
  setText(text);
  this.setEnabled(false);

  setFont();
  setEnabled(true);
 }

 private void setFont () {
  TextAppearance appearance = this.getAppearance();
  TextStyle def = new TextStyle();
  def.getTextStyleEntry(TextStyle.DEFAULTSTYLEKEY).setColor(Color.WHITE);
  appearance.addStyle(TextStyle.DEFAULTSTYLEKEY, def);

  ITextRenderer renderer = appearance.getRenderer(
     ITextRenderer.DEFAULTTEXTRENDERERKEY).copy();
  Font cust = null;
  try {
   cust = Font.createFont(Font.TRUETYPE_FONT, new File(
                          Main.DIR + "res/font.ttf")).deriveFont(14f);
  } catch ( FontFormatException | IOException ex ) {
   Main.LOG.addE(ex);
  }

  renderer.setFont(FontFactory.renderStandardFont(cust, true, Alphabet.
                                                  getDefaultAlphabet()));
  appearance.addRenderer(ITextRenderer.DEFAULTTEXTRENDERERKEY, renderer);
 }
}
