// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.tools.ListenerList;

/**
 * Class to hold {@link ImageEntry} and the current selection
 * @since 14590
 */
public class ImageData {
    /**
     * A listener that is informed when the current selection change
     */
    public interface ImageDataUpdateListener {
        /**
         * Called when the data change
         * @param data the image data
         */
        void imageDataUpdated(ImageData data);

        /**
         * Called when the selection change
         * @param data the image data
         */
        void selectedImageChanged(ImageData data);
    }

    private final List<ImageEntry> data;

    private int selectedImageIndex = -1;

    private final ListenerList<ImageDataUpdateListener> listeners = ListenerList.create();

    /**
     * Construct a new image container without images
     */
    public ImageData() {
        this(null);
    }

    /**
     * Construct a new image container with a list of images
     * @param data the list of {@link ImageEntry}
     */
    public ImageData(List<ImageEntry> data) {
        if (data != null) {
            Collections.sort(data);
            this.data = data;
        } else {
            this.data = new ArrayList<>();
        }
    }

    /**
     * Returns the images
     * @return the images
     */
    public List<ImageEntry> getImages() {
        return data;
    }

    /**
     * Determines if one image has modified GPS data.
     * @return {@code true} if data has been modified; {@code false}, otherwise
     */
    public boolean isModified() {
        for (ImageEntry e : data) {
            if (e.hasNewGpsData()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Merge 2 ImageData
     * @param otherData {@link ImageData} to merge
     */
    public void mergeFrom(ImageData otherData) {
        data.addAll(otherData.getImages());
        Collections.sort(data);

        final ImageEntry selected = otherData.getSelectedImage();

        // Suppress the double photos.
        if (data.size() > 1) {
            ImageEntry prev = data.get(data.size() - 1);
            for (int i = data.size() - 2; i >= 0; i--) {
                ImageEntry cur = data.get(i);
                if (cur.getFile().equals(prev.getFile())) {
                    data.remove(i);
                } else {
                    prev = cur;
                }
            }
        }
        if (selected != null) {
            setSelectedImageIndex(data.indexOf(selected));
        }
    }

    /**
     * Return the current selected image
     * @return the selected image as {@link ImageEntry} or null
     */
    public ImageEntry getSelectedImage() {
        if (selectedImageIndex > -1) {
            return data.get(selectedImageIndex);
        }
        return null;
    }

    /**
     * Select the first image of the sequence
     */
    public void selectFirstImage() {
        if (!data.isEmpty()) {
            setSelectedImageIndex(0);
        }
    }

    /**
     * Select the last image of the sequence
     */
    public void selectLastImage() {
        setSelectedImageIndex(data.size() - 1);
    }

    /**
     * Check if there is a next image in the sequence
     * @return {@code true} is there is a next image, {@code false} otherwise
     */
    public boolean hasNextImage() {
        return selectedImageIndex != data.size() - 1;
    }

    /**
     * Select the next image of the sequence
     */
    public void selectNextImage() {
        if (hasNextImage()) {
            setSelectedImageIndex(selectedImageIndex + 1);
        }
    }

    /**
     *  Check if there is a previous image in the sequence
     * @return {@code true} is there is a previous image, {@code false} otherwise
     */
    public boolean hasPreviousImage() {
        return selectedImageIndex - 1 > -1;
    }

    /**
     * Select the previous image of the sequence
     */
    public void selectPreviousImage() {
        if (data.isEmpty()) {
            return;
        }
        setSelectedImageIndex(Integer.max(0, selectedImageIndex - 1));
    }

    /**
     * Select as the selected the given image
     * @param image the selected image
     */
    public void setSelectedImage(ImageEntry image) {
        setSelectedImageIndex(data.indexOf(image));
    }

    /**
     * Clear the selected image
     */
    public void clearSelectedImage() {
        setSelectedImageIndex(-1);
    }

    private void setSelectedImageIndex(int index) {
        setSelectedImageIndex(index, false);
    }

    private void setSelectedImageIndex(int index, boolean forceTrigger) {
        if (index == selectedImageIndex && !forceTrigger) {
            return;
        }
        selectedImageIndex = index;
        listeners.fireEvent(l -> l.selectedImageChanged(this));
    }

    /**
     * Remove the current selected image from the list
     */
    public void removeSelectedImage() {
        data.remove(getSelectedImage());
        if (selectedImageIndex == data.size()) {
            setSelectedImageIndex(data.size() - 1);
        } else {
            setSelectedImageIndex(selectedImageIndex, true);
        }
    }

    /**
     * Remove the image from the list and trigger update listener
     * @param img the {@link ImageEntry} to remove
     */
    public void removeImage(ImageEntry img) {
        data.remove(img);
        notifyImageUpdate();
    }

    /**
     * Update the position of the image and trigger update
     * @param img the image to update
     * @param newPos the new position
     */
    public void updateImagePosition(ImageEntry img, LatLon newPos) {
        img.setPos(newPos);
        afterImageUpdated(img);
    }

    /**
     * Update the image direction of the image and trigger update
     * @param img the image to update
     * @param direction the new direction
     */
    public void updateImageDirection(ImageEntry img, double direction) {
        img.setExifImgDir(direction);
        afterImageUpdated(img);
    }

    /**
     * Manually trigger the {@link ImageDataUpdateListener#imageDataUpdated(ImageData)}
     */
    public void notifyImageUpdate() {
        listeners.fireEvent(l -> l.imageDataUpdated(this));
    }

    private void afterImageUpdated(ImageEntry img) {
        img.flagNewGpsData();
        notifyImageUpdate();
    }

    /**
     * Add a listener that listens to image data changes
     * @param listener the {@link ImageDataUpdateListener}
     */
    public void addImageDataUpdateListener(ImageDataUpdateListener listener) {
        listeners.addListener(listener);
    }

    /**
     * Removes a listener that listens to image data changes
     * @param listener The listener
     */
    public void removeImageDataUpdateListener(ImageDataUpdateListener listener) {
        listeners.removeListener(listener);
    }
}
