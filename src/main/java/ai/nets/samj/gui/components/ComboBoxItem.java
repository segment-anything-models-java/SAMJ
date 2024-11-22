/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.gui.components;


import java.util.UUID;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
/**
 * Class that allows identifying the objects in the list of a ComboBox by a unique identifier.
 * This class helps associating a unique identifier to objects specific to a software.
 * This class needs to be implemented on each software to be able to handle the native structures for images
 * from the particualr software.
 * @author CArlos Garcia
 */
public abstract class ComboBoxItem {
    final private String id;
    final private Object value;
    private static final String SELECT_IMAGE_STR = "Select image";

    /**
     * An object that associates a unique identifier with an Object.
     * The unique identifier cannot be -1, as it is reserved for empty objects
     * @param uniqueID
     * 	the unique identifier
     * @param seq
     * 	the object
     */
    public ComboBoxItem(Object seq) {
        this.id = UUID.randomUUID().toString();
        this.value = seq;
    }

    /**
     * An object that associates a unique identifier with an Object.
     * In this case it is empty, the unique identifier will by -1.
     */
    public ComboBoxItem() {
        this.id = "-1";
        this.value = null;
    }

    /**
     * 
     * @return the unique identifier of the instance
     */
    public String getId() {
        return id;
    }

    /**
     * 
     * @return the Object of interest
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * 
     * @return the name of the image. The way to retrieve the image (Object) name depends on the software where this is used,
     * 	so it needs to be implemented on the software side
     */
    public abstract String getImageName();
    
    /**
     * Convert the software specific image into an ImgLib2 {@link RandomAccessibleInterval}
     * @param <T>
     * 	the possible ImgLib2 data types of the {@link RandomAccessibleInterval}
     * @return the {@link RandomAccessibleInterval} created from the software image object
     */
    public abstract < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval<T> getImageAsImgLib2();

    @Override
    /**
     * {@inheritDoc}
     */
    public String toString() {
    	if (value == null) return SELECT_IMAGE_STR;
        return getImageName();
    }
}
