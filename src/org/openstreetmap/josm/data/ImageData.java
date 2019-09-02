// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    private final List<Integer> selectedImagesIndex = new ArrayList<>();

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
        selectedImagesIndex.add(-1);
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
     * Return the first currently selected image
     * @return the first selected image as {@link ImageEntry} or null
     * @see #getSelectedImages
     */
    public ImageEntry getSelectedImage() {
        int selectedImageIndex = selectedImagesIndex.isEmpty() ? -1 : selectedImagesIndex.get(0);
        if (selectedImageIndex > -1) {
            return data.get(selectedImageIndex);
        }
        return null;
    }

    /**
     * Return the current selected images
     * @return the selected images as list {@link ImageEntry}
     * @since 15333
     */
    public List<ImageEntry> getSelectedImages() {
        return selectedImagesIndex.stream().filter(i -> i > -1).map(data::get).collect(Collectors.toList());
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
        return (selectedImagesIndex.size() == 1 && selectedImagesIndex.get(0) != data.size() - 1);
    }

    /**
     * Select the next image of the sequence
     */
    public void selectNextImage() {
        if (hasNextImage()) {
            setSelectedImageIndex(selectedImagesIndex.get(0) + 1);
        }
    }

    /**
     *  Check if there is a previous image in the sequence
     * @return {@code true} is there is a previous image, {@code false} otherwise
     */
    public boolean hasPreviousImage() {
        return (selectedImagesIndex.size() == 1 && selectedImagesIndex.get(0) - 1 > -1);
    }

    /**
     * Select the previous image of the sequence
     */
    public void selectPreviousImage() {
        if (data.isEmpty()) {
            return;
        }
        setSelectedImageIndex(Integer.max(0, selectedImagesIndex.get(0) - 1));
    }

    /**
     * Select as the selected the given image
     * @param image the selected image
     */
    public void setSelectedImage(ImageEntry image) {
        setSelectedImageIndex(data.indexOf(image));
    }

    /**
     * Add image to the list of selected images
     * @param image {@link ImageEntry} the image to add
     * @since 15333
     */
    public void addImageToSelection(ImageEntry image) {
        int index = data.indexOf(image);
        if (selectedImagesIndex.get(0) == -1) {
            setSelectedImage(image);
        } else if (!selectedImagesIndex.contains(index)) {
            selectedImagesIndex.add(index);
            listeners.fireEvent(l -> l.selectedImageChanged(this));
        }
    }

    /**
     * Remove the image from the list of selected images
     * @param image {@link ImageEntry} the image to remove
     * @since 15333
     */
    public void removeImageToSelection(ImageEntry image) {
        int index = data.indexOf(image);
        selectedImagesIndex.remove(selectedImagesIndex.indexOf(index));
        if (selectedImagesIndex.isEmpty()) {
            selectedImagesIndex.add(-1);
        }
        listeners.fireEvent(l -> l.selectedImageChanged(this));
    }

    /**
     * Clear the selected image(s)
     */
    public void clearSelectedImage() {
        setSelectedImageIndex(-1);
    }

    private void setSelectedImageIndex(int index) {
        setSelectedImageIndex(index, false);
    }

    private void setSelectedImageIndex(int index, boolean forceTrigger) {
        if (selectedImagesIndex.size() > 1) {
            selectedImagesIndex.clear();
            selectedImagesIndex.add(-1);
        }
        if (index == selectedImagesIndex.get(0) && !forceTrigger) {
            return;
        }
        selectedImagesIndex.set(0, index);
        listeners.fireEvent(l -> l.selectedImageChanged(this));
    }

    /**
     * Remove the current selected image from the list
     */
    public void removeSelectedImage() {
        List<ImageEntry> selected = getSelectedImages();
        if (selected.size() > 1) {
            throw new IllegalStateException(tr("Multiple images have been selected"));
        }
        if (selected.isEmpty()) {
            return;
        }
        data.remove(getSelectedImages().get(0));
        if (selectedImagesIndex.get(0) == data.size()) {
            setSelectedImageIndex(data.size() - 1);
        } else {
            setSelectedImageIndex(selectedImagesIndex.get(0), true);
        }
    }

    /**
     * Determines if the image is selected
     * @param image the {@link ImageEntry} image
     * @return {@code true} is the image is selected, {@code false} otherwise
     * @since 15333
     */
    public boolean isImageSelected(ImageEntry image) {
        int index = data.indexOf(image);
        return selectedImagesIndex.contains(index);
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
