package com.imageeditor.operation;

import java.awt.image.BufferedImage;

public interface Operation {
    BufferedImage apply(BufferedImage image);
}
