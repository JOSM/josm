// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxImageEntry;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.tools.ListenerList;

/**
 * Class to hold {@link ImageEntry} and the current selection
 * @since 14590
 */
public class ImageData implements Data {
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
    private final QuadBuckets<ImageEntry> geoImages = new QuadBuckets<>();
    private Layer layer;

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
            this.data.forEach(image -> image.setDataSet(this));
        } else {
            this.data = new ArrayList<>();
        }
        this.geoImages.addAll(this.data);
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
        return data.stream().anyMatch(GpxImageEntry::hasNewGpsData);
    }

    /**
     * Merge 2 ImageData
     * @param otherData {@link ImageData} to merge
     */
    public void mergeFrom(ImageData otherData) {
        data.addAll(otherData.getImages());
        this.geoImages.addAll(otherData.getImages());
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
        return selectedImagesIndex.stream().filter(i -> i > -1 && i < data.size()).map(data::get).collect(Collectors.toList());
    }

    /**
     * Get the first image on the layer
     * @return The first image
     * @since 18246
     */
    public ImageEntry getFirstImage() {
        if (!this.data.isEmpty()) {
            return this.data.get(0);
        }
        return null;
    }

    /**
     * Get the last image in the layer
     * @return The last image
     * @since 18246
     */
    public ImageEntry getLastImage() {
        if (!this.data.isEmpty()) {
            return this.data.get(this.data.size() - 1);
        }
        return null;
    }

    /**
     * Check if there is a next image in the sequence
     * @return {@code true} is there is a next image, {@code false} otherwise
     */
    public boolean hasNextImage() {
        return (selectedImagesIndex.size() == 1 && selectedImagesIndex.get(0) != data.size() - 1);
    }

    /**
     * Search for images in a bounds
     * @param bounds The bounds to search
     * @return images in the bounds
     * @since 17459
     */
    public Collection<ImageEntry> searchImages(Bounds bounds) {
        return this.geoImages.search(bounds.toBBox());
    }

    /**
     * Get the image next to the current image
     * @return The next image
     * @since 18246
     */
    public ImageEntry getNextImage() {
        if (this.hasNextImage()) {
            return this.data.get(this.selectedImagesIndex.get(0) + 1);
        }
        return null;
    }

    /**
     * Get the image previous to the current image
     * @return The previous image
     * @since 18246
     */
    public ImageEntry getPreviousImage() {
        if (this.hasPreviousImage()) {
            return this.data.get(Integer.max(0, selectedImagesIndex.get(0) - 1));
        }
        return null;
    }

    /**
     *  Check if there is a previous image in the sequence
     * @return {@code true} is there is a previous image, {@code false} otherwise
     */
    public boolean hasPreviousImage() {
        return (selectedImagesIndex.size() == 1 && selectedImagesIndex.get(0) - 1 > -1);
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
     * Indicate that an entry has changed
     * @param gpxImageEntry The entry to update
     * @since 17574
     */
    public void fireNodeMoved(ImageEntry gpxImageEntry) {
        this.geoImages.remove(gpxImageEntry);
        this.geoImages.add(gpxImageEntry);
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
     * @since 15348
     */
    public void removeSelectedImages() {
        removeImages(getSelectedImages());
    }

    private void removeImages(List<ImageEntry> selectedImages) {
        if (selectedImages.isEmpty()) {
            return;
        }
        for (ImageEntry img: getSelectedImages()) {
            removeImage(img, false);
        }
        updateSelectedImage();
    }

    /**
     * Update the selected image after removal of one or more images.
     * @since 18049
     */
    public void updateSelectedImage() {
        int size = data.size();
        Integer firstSelectedImageIndex = selectedImagesIndex.get(0);
        if (firstSelectedImageIndex >= size) {
            setSelectedImageIndex(size - 1);
        } else {
            setSelectedImageIndex(firstSelectedImageIndex, true);
        }
    }

    /**
     * Determines if the image is selected
     * @param image the {@link ImageEntry} image
     * @return {@code true} is the image is selected, {@code false} otherwise
     * @since 15333
     */
    public boolean isImageSelected(ImageEntry image) {
        return selectedImagesIndex.contains(data.indexOf(image));
    }

    /**
     * Remove the image from the list and trigger update listener
     * @param img the {@link ImageEntry} to remove
     */
    public void removeImage(ImageEntry img) {
        removeImage(img, true);
    }

    /**
     * Remove the image from the list and optionally trigger update listener
     * @param img the {@link ImageEntry} to remove
     * @param fireUpdateEvent if {@code true}, notifies listeners of image update
     * @since 18049
     */
    public void removeImage(ImageEntry img, boolean fireUpdateEvent) {
        data.remove(img);
        this.geoImages.remove(img);
        if (fireUpdateEvent) {
            notifyImageUpdate();
            // Fix JOSM #21521 -- when an image is removed, we need to update the selected image
            this.updateSelectedImage();
        }
    }

    /**
     * Update the position of the image and trigger update
     * @param img the image to update
     * @param newPos the new position
     */
    public void updateImagePosition(ImageEntry img, LatLon newPos) {
        img.setPos(newPos);
        this.geoImages.remove(img);
        this.geoImages.add(img);
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
     * Update the GPS track direction of the image and trigger update.
     * @param img the image to update
     * @param trackDirection the new GPS track direction
     * @since 19387
     */
    public void updateImageGpsTrack(ImageEntry img, double trackDirection) {
        img.setExifGpsTrack(trackDirection);
        afterImageUpdated(img);
    }

    /**
     * Update the image horizontal positioning error and trigger update.
     * @param img the image to update
     * @param hposerr the new horizontal positionning error
     * @since 19387
     */
    public void updateImageHPosErr(ImageEntry img, double hposerr) {
        img.setExifHPosErr(hposerr);
        afterImageUpdated(img);
    }

    /**
     * Update the image GPS differential mode and trigger update.
     * @param img the image to update
     * @param gpsDiffMode the new GPS differential mode
     * @since 19387
     */
    public void updateImageGpsDiffMode(ImageEntry img, Integer gpsDiffMode) {
        img.setGpsDiffMode(gpsDiffMode);
        afterImageUpdated(img);
    }

    /**
     * Update the image GPS 2d/3d mode value and trigger update.
     * @param img the image to update
     * @param gps2d3dMode the new 2d/3d GPS mode
     * @since 19387
     */
    public void updateImageGps2d3dMode(ImageEntry img, Integer gps2d3dMode) {
        img.setGps2d3dMode(gps2d3dMode);
        afterImageUpdated(img);
    }

    /**
     * Update the image GPS DOP value and trigger update.
     * @param img the image to update
     * @param exifGpsDop the new GPS DOP value
     * @since 19387
     */
    public void updateImageExifGpsDop(ImageEntry img, Double exifGpsDop) {
        img.setExifGpsDop(exifGpsDop);
        afterImageUpdated(img);
    }

    /**
     * Update the image GPS datum and trigger update.
     * @param img the image to update
     * @param exifGpsDatum the new datum string value
     * @since 19387
     */
    public void updateImageExifGpsDatum(ImageEntry img, String exifGpsDatum) {
        img.setExifGpsDatum(exifGpsDatum);
        afterImageUpdated(img);
    }

    /**
     * Update the image GPS processing method and trigger update.
     * @param img the image to update
     * @param exifGpsProcMethod the new GPS processing method
     * @since 19387
     */
    public void updateImageExifGpsProcMethod(ImageEntry img, String exifGpsProcMethod) {
        img.setExifGpsProcMethod(exifGpsProcMethod);
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
     * Set the layer for use with {@link org.openstreetmap.josm.gui.layer.geoimage.ImageViewerDialog#displayImages(List)}
     * @param layer The layer to use for organization
     * @since 18591
     */
    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    /**
     * Get the layer that this data is associated with. May be {@code null}.
     * @return The layer this data is associated with.
     * @since 18591
     */
    public Layer getLayer() {
        return this.layer;
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

    @Override
    public Collection<DataSource> getDataSources() {
        return Collections.emptyList();
    }
}
