// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * A remote image entry
 * @since 18592
 */
public class RemoteEntry implements IImageEntry<RemoteEntry>, ImageMetadata {
    private final URI uri;
    private final Supplier<RemoteEntry> firstImage;
    private final Supplier<RemoteEntry> nextImage;
    private final Supplier<RemoteEntry> previousImage;
    private final Supplier<RemoteEntry> lastImage;
    private final Layer layer;
    private int width;
    private int height;
    private ILatLon pos;
    private Integer exifOrientation;
    private Double elevation;
    private Double speed;
    private Double exifImgDir;
    private ILatLon exifCoor;
    private Instant exifTime;
    private Instant exifGpsTime;
    private Instant gpsTime;
    private String iptcObjectName;
    private List<String> iptcKeywords;
    private String iptcHeadline;
    private String iptcCaption;
    private Projections projection;
    private String title;

    /**
     * Create a new remote entry
     * @param layer The originating layer, used for tabs in the image viewer
     * @param uri The URI to use
     * @param firstImage first image supplier
     * @param nextImage next image supplier
     * @param lastImage last image supplier
     * @param previousImage previous image supplier
     */
    public RemoteEntry(Layer layer, URI uri, Supplier<RemoteEntry> firstImage, Supplier<RemoteEntry> previousImage,
                       Supplier<RemoteEntry> nextImage, Supplier<RemoteEntry> lastImage) {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(firstImage);
        Objects.requireNonNull(previousImage);
        Objects.requireNonNull(nextImage);
        Objects.requireNonNull(lastImage);
        this.uri = uri;
        this.firstImage = firstImage;
        this.previousImage = previousImage;
        this.nextImage = nextImage;
        this.lastImage = lastImage;
        this.layer = layer;
    }

    @Override
    public RemoteEntry getNextImage() {
        return this.nextImage.get();
    }

    @Override
    public RemoteEntry getPreviousImage() {
        return this.previousImage.get();
    }

    @Override
    public RemoteEntry getFirstImage() {
        return this.firstImage.get();
    }

    @Override
    public RemoteEntry getLastImage() {
        return this.lastImage.get();
    }

    @Override
    public String getDisplayName() {
        return this.title == null ? this.getImageURI().toString() : this.title;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void setPos(ILatLon pos) {
        this.pos = pos;
    }

    @Override
    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    @Override
    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    @Override
    public void setExifOrientation(Integer exifOrientation) {
        this.exifOrientation = exifOrientation;
    }

    @Override
    public void setExifTime(Instant exifTime) {
        this.exifTime = exifTime;
    }

    @Override
    public void setExifGpsTime(Instant exifGpsTime) {
        this.exifGpsTime = exifGpsTime;
    }

    @Override
    public void setGpsTime(Instant gpsTime) {
        this.gpsTime = gpsTime;
    }

    @Override
    public void setExifCoor(ILatLon exifCoor) {
        this.exifCoor = exifCoor;
    }

    @Override
    public void setExifImgDir(Double exifDir) {
        this.exifImgDir = exifDir;
    }

    @Override
    public void setIptcCaption(String iptcCaption) {
        this.iptcCaption = iptcCaption;
    }

    @Override
    public void setIptcHeadline(String iptcHeadline) {
        this.iptcHeadline = iptcHeadline;
    }

    @Override
    public void setIptcKeywords(List<String> iptcKeywords) {
        this.iptcKeywords = iptcKeywords;
    }

    @Override
    public void setIptcObjectName(String iptcObjectName) {
        this.iptcObjectName = iptcObjectName;
    }

    @Override
    public Integer getExifOrientation() {
        return this.exifOrientation != null ? this.exifOrientation : 1;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public URI getImageURI() {
        return this.uri;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public ILatLon getPos() {
        return this.pos;
    }

    @Override
    public Double getSpeed() {
        return this.speed;
    }

    @Override
    public Double getElevation() {
        return this.elevation;
    }

    @Override
    public Double getExifImgDir() {
        return this.exifImgDir;
    }

    @Override
    public Instant getLastModified() {
        if (this.getImageURI().getScheme().contains("file:")) {
            try {
                return Files.getLastModifiedTime(Paths.get(this.getImageURI())).toInstant();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try {
            return Instant.ofEpochMilli(HttpClient.create(this.getImageURI().toURL(), "HEAD").getResponse().getLastModified());
        } catch (MalformedURLException e) {
            throw new JosmRuntimeException(e);
        }
    }

    @Override
    public boolean hasExifTime() {
        return this.exifTime != null;
    }

    @Override
    public Instant getExifGpsInstant() {
        return this.exifGpsTime;
    }

    @Override
    public boolean hasExifGpsTime() {
        return this.exifGpsTime != null;
    }

    @Override
    public ILatLon getExifCoor() {
        return this.exifCoor;
    }

    @Override
    public Instant getExifInstant() {
        return this.exifTime;
    }

    @Override
    public boolean hasGpsTime() {
        return this.gpsTime != null;
    }

    @Override
    public Instant getGpsInstant() {
        return this.gpsTime;
    }

    @Override
    public String getIptcCaption() {
        return this.iptcCaption;
    }

    @Override
    public String getIptcHeadline() {
        return this.iptcHeadline;
    }

    @Override
    public List<String> getIptcKeywords() {
        return this.iptcKeywords;
    }

    @Override
    public String getIptcObjectName() {
        return this.iptcObjectName;
    }

    @Override
    public Projections getProjectionType() {
        return this.projection;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        URI u = getImageURI();
        if (u.getScheme().contains("file")) {
            return Files.newInputStream(Paths.get(u));
        }
        HttpClient client = HttpClient.create(u.toURL());
        InputStream actual = client.connect().getContent();
        return new BufferedInputStream(actual) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    client.disconnect();
                }
            }
        };
    }

    @Override
    public void selectImage(ImageViewerDialog imageViewerDialog, IImageEntry<?> entry) {
        imageViewerDialog.displayImages(this.layer, Collections.singletonList(entry));
    }

    @Override
    public void setProjectionType(Projections newProjection) {
        this.projection = newProjection;
    }

    /**
     * Set the display name for this entry
     * @param text The display name
     */
    public void setDisplayName(String text) {
        this.title = text;
    }
}
