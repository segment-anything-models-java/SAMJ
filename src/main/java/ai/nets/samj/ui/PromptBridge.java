package ai.nets.samj.ui;

import java.awt.Rectangle;
import java.util.List;

import ai.nets.samj.annotation.Mask;
import net.imglib2.Localizable;

public class PromptBridge {

    public  List<Mask> sendRectanglePrompt(long[] xywh) {
    	return null;
    }

    public List<Mask> sentPointPrompt(List<Localizable> posPoints) {
        return null;
    }

    public List<Mask> sentPointPrompt(List<Localizable> posPoints, Rectangle zoomedArea) {
        return null;
    }

    public List<Mask> sentPointPrompt(List<Localizable> posPoints, List<Localizable> negPoints) {
        return null;
    }

    public List<Mask> sentPointPrompt(List<Localizable> posPoints, List<Localizable> negPoints, Rectangle zoomedArea) {
        return null;
    }

    public void notifyCloseModel() {
        
    }
}