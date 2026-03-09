package ai.nets.samj.ui;

import java.awt.Rectangle;
import java.util.List;

import ai.nets.samj.annotation.Mask;
import net.imglib2.Localizable;

public abstract class PromptBridge {

    public abstract List<Mask> sendRectanglePrompt(long[] xywh);

    public abstract List<Mask> sentPointPrompt(List<Localizable> posPoints);

    public abstract List<Mask> sentPointPrompt(List<Localizable> posPoints, Rectangle zoomedArea);

    public abstract List<Mask> sentPointPrompt(List<Localizable> posPoints, List<Localizable> negPoints);

    public abstract List<Mask> sentPointPrompt(List<Localizable> posPoints, List<Localizable> negPoints, Rectangle zoomedArea);

    public abstract void notifyCloseModel();
}