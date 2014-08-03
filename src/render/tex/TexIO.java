package render.tex;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import jogamp.opengl.Debug;
import render.tex.spi.DDSImage;
import render.tex.spi.JPEGImage;
import render.tex.spi.NetPbmTexWriter;
import render.tex.spi.PNGImage;
import render.tex.spi.SGIImage;
import render.tex.spi.TGAImage;
import render.tex.spi.TexProvider;
import render.tex.spi.TexWriter;

/** <P> Provides input and output facilities for both loading OpenGL
    textures from disk and streams as well as writing textures already
    in memory back to disk. </P>

    <P> The TextureIO class supports an arbitrary number of plug-in
    readers and writers via TextureProviders and TextureWriters.
    TextureProviders know how to produce TexData objects from
    files, InputStreams and URLs. TextureWriters know how to write
    TexData objects to disk in various file formats. The
    TexData class represents the raw data of the texture before it
    has been converted to an OpenGL texture object. The Texture class
    represents the OpenGL texture object and provides easy facilities
    for using the texture. </P>

    <P> There are several built-in TextureProviders and TextureWriters
    supplied with the TextureIO implementation. The most basic
    provider uses the platform's Image I/O facilities to read in a
    BufferedImage and convert it to a texture. This is the baseline
    provider and is registered so that it is the last one consulted.
    All others are asked first to open a given file. </P>

    <P> There are three other providers registered by default as of
    the time of this writing. One handles SGI RGB (".sgi", ".rgb")
    images from both files and streams. One handles DirectDraw Surface
    (".dds") images read from files, though can not read these images
    from streams. One handles Targa (".tga") images read from both
    files and streams. These providers are executed in an arbitrary
    order. Some of these providers require the file's suffix to either
    be specified via the newTexData methods or for the file to be
    named with the appropriate suffix. In general a file suffix should
    be provided to the newTexture and newTexData methods if at all
    possible. </P>

    <P> Note that additional TextureProviders, if reading images from
    InputStreams, must use the mark()/reset() methods on InputStream
    when probing for e.g. magic numbers at the head of the file to
    make sure not to disturb the state of the InputStream for
    downstream TextureProviders. </P>

    <P> There are analogous TextureWriters provided for writing
    textures back to disk if desired. As of this writing, there are
    four TextureWriters registered by default: one for Targa files,
    one for SGI RGB files, one for DirectDraw surface (.dds) files,
    and one for ImageIO-supplied formats such as .jpg and .png.  Some
    of these writers have certain limitations such as only being able
    to write out textures stored in GL_RGB or GL_RGBA format. The DDS
    writer supports fetching and writing to disk of texture data in
    DXTn compressed format. Whether this will occur is dependent on
    whether the texture's internal format is one of the DXTn
    compressed formats and whether the target file is .dds format.
*/

public class TexIO {
    /** Constant which can be used as a file suffix to indicate a
        DirectDraw Surface file. */
    public static final String DDS     = "dds";

    /** Constant which can be used as a file suffix to indicate an SGI
        RGB file. */
    public static final String SGI     = "sgi";

    /** Constant which can be used as a file suffix to indicate an SGI
        RGB file. */
    public static final String SGI_RGB = "rgb";

    /** Constant which can be used as a file suffix to indicate a GIF
        file. */
    public static final String GIF     = "gif";

    /** Constant which can be used as a file suffix to indicate a JPEG
        file. */
    public static final String JPG     = "jpg";

    /** Constant which can be used as a file suffix to indicate a PNG
        file. */
    public static final String PNG     = "png";

    /** Constant which can be used as a file suffix to indicate a Targa
        file. */
    public static final String TGA     = "tga";

    /** Constant which can be used as a file suffix to indicate a TIFF
        file. */
    public static final String TIFF    = "tiff";

    /** Constant which can be used as a file suffix to indicate a PAM
        file, NetPbm magic 7 - binary RGB and RGBA. Write support only. */
    public static final String PAM     = "pam";

    /** Constant which can be used as a file suffix to indicate a PAM
        file, NetPbm magic 6 - binary RGB. Write support only. */
    public static final String PPM     = "ppm";

    private static final boolean DEBUG = Debug.debug("TextureIO");

    // For manually disabling the use of the texture rectangle
    // extensions so you know the texture target is GL_TEXTURE_2D; this
    // is useful for shader writers (thanks to Chris Campbell for this
    // observation)
    private static boolean texRectEnabled = true;

    //----------------------------------------------------------------------
    // methods that *do not* require a current context
    // These methods assume RGB or RGBA textures.
    // Some texture providers may not recognize the file format unless
    // the fileSuffix is specified, so it is strongly recommended to
    // specify it wherever it is known.
    // Some texture providers may also only support one kind of input,
    // i.e., reading from a file as opposed to a stream.

    /**
     * Creates a TexData from the given file. Does no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param file the file from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the file, or null if none of the
     *         registered texture providers could read the file
     * @throws IOException if an error occurred while reading the file
     */
    public static TexData newTexData(GLProfile glp, File file,
                                             boolean mipmap,
                                             String fileSuffix) throws IOException {
        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(file);
        }
        return newTexDataImpl(glp, file, 0, 0, mipmap, fileSuffix);
    }

    /**
     * Creates a TexData from the given stream. Does no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param stream the stream from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the stream, or null if none of the
     *         registered texture providers could read the stream
     * @throws IOException if an error occurred while reading the stream
     */
    public static TexData newTexData(GLProfile glp, InputStream stream,
                                             boolean mipmap,
                                             String fileSuffix) throws IOException {
        return newTexDataImpl(glp, stream, 0, 0, mipmap, fileSuffix);
    }

    /**
     * Creates a TexData from the given URL. Does no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param url the URL from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the URL, or null if none of the
     *         registered texture providers could read the URL
     * @throws IOException if an error occurred while reading the URL
     */
    public static TexData newTexData(GLProfile glp, URL url,
                                             boolean mipmap,
                                             String fileSuffix) throws IOException {
        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(url.getPath());
        }
        return newTexDataImpl(glp, url, 0, 0, mipmap, fileSuffix);
    }

    //----------------------------------------------------------------------
    // These methods make no assumption about the OpenGL internal format
    // or pixel format of the texture; they must be specified by the
    // user. It is not allowed to supply 0 (indicating no preference)
    // for either the internalFormat or the pixelFormat;
    // IllegalArgumentException will be thrown in this case.

    /**
     * Creates a TexData from the given file, using the specified
     * OpenGL internal format and pixel format for the texture which
     * will eventually result. The internalFormat and pixelFormat must
     * be specified and may not be zero; to use default values, use the
     * variant of this method which does not take these arguments. Does
     * no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param file the file from which to read the texture data
     * @param internalFormat the OpenGL internal format of the texture
     *                   which will eventually result from the TexData
     * @param pixelFormat the OpenGL pixel format of the texture
     *                    which will eventually result from the TexData
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the file, or null if none of the
     *         registered texture providers could read the file
     * @throws IllegalArgumentException if either internalFormat or
     *                                  pixelFormat was 0
     * @throws IOException if an error occurred while reading the file
     */
    public static TexData newTexData(GLProfile glp, File file,
                                             int internalFormat,
                                             int pixelFormat,
                                             boolean mipmap,
                                             String fileSuffix) throws IOException, IllegalArgumentException {
        if ((internalFormat == 0) || (pixelFormat == 0)) {
            throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
        }

        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(file);
        }

        return newTexDataImpl(glp, file, internalFormat, pixelFormat, mipmap, fileSuffix);
    }

    /**
     * Creates a TexData from the given stream, using the specified
     * OpenGL internal format and pixel format for the texture which
     * will eventually result. The internalFormat and pixelFormat must
     * be specified and may not be zero; to use default values, use the
     * variant of this method which does not take these arguments. Does
     * no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param stream the stream from which to read the texture data
     * @param internalFormat the OpenGL internal format of the texture
     *                   which will eventually result from the TexData
     * @param pixelFormat the OpenGL pixel format of the texture
     *                    which will eventually result from the TexData
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the stream, or null if none of the
     *         registered texture providers could read the stream
     * @throws IllegalArgumentException if either internalFormat or
     *                                  pixelFormat was 0
     * @throws IOException if an error occurred while reading the stream
     */
    public static TexData newTexData(GLProfile glp, InputStream stream,
                                             int internalFormat,
                                             int pixelFormat,
                                             boolean mipmap,
                                             String fileSuffix) throws IOException, IllegalArgumentException {
        if ((internalFormat == 0) || (pixelFormat == 0)) {
            throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
        }

        return newTexDataImpl(glp, stream, internalFormat, pixelFormat, mipmap, fileSuffix);
    }

    /**
     * Creates a TexData from the given URL, using the specified
     * OpenGL internal format and pixel format for the texture which
     * will eventually result. The internalFormat and pixelFormat must
     * be specified and may not be zero; to use default values, use the
     * variant of this method which does not take these arguments. Does
     * no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param url the URL from which to read the texture data
     * @param internalFormat the OpenGL internal format of the texture
     *                   which will eventually result from the TexData
     * @param pixelFormat the OpenGL pixel format of the texture
     *                    which will eventually result from the TexData
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the URL, or null if none of the
     *         registered texture providers could read the URL
     * @throws IllegalArgumentException if either internalFormat or
     *                                  pixelFormat was 0
     * @throws IOException if an error occurred while reading the URL
     */
    public static TexData newTexData(GLProfile glp, URL url,
                                             int internalFormat,
                                             int pixelFormat,
                                             boolean mipmap,
                                             String fileSuffix) throws IOException, IllegalArgumentException {
        if ((internalFormat == 0) || (pixelFormat == 0)) {
            throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
        }

        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(url.getPath());
        }

        return newTexDataImpl(glp, url, internalFormat, pixelFormat, mipmap, fileSuffix);
    }

    //----------------------------------------------------------------------
    // methods that *do* require a current context
    //

    /**
     * Creates an OpenGL texture object from the specified TexData
     * using the current OpenGL context.
     *
     * @param data the texture data to turn into an OpenGL texture
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     * @throws IllegalArgumentException if the passed TexData was null
     */
    public static Tex newTexture(TexData data) throws GLException, IllegalArgumentException {
        return newTexture(GLContext.getCurrentGL(), data);
    }

    /**
     * Creates an OpenGL texture object from the specified TexData
     * using the given OpenGL context.
     *
     * @param data the texture data to turn into an OpenGL texture
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     * @throws IllegalArgumentException if the passed TexData was null
     */
    public static Tex newTexture(GL gl, TexData data) throws GLException, IllegalArgumentException {
        if (data == null) {
            throw new IllegalArgumentException("Null TexData");
        }
        return new Tex(gl, data);
    }

    /**
     * Creates an OpenGL texture object from the specified file using
     * the current OpenGL context.
     *
     * @param file the file from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @throws IOException if an error occurred while reading the file
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     */
    public static Tex newTexture(File file, boolean mipmap) throws IOException, GLException {
        GL gl = GLContext.getCurrentGL();
        GLProfile glp = gl.getGLProfile();
        TexData data = newTexData(glp, file, mipmap, IOUtil.getFileSuffix(file));
        Tex texture = newTexture(gl, data);
        data.flush();
        return texture;
    }

    /**
     * Creates an OpenGL texture object from the specified stream using
     * the current OpenGL context.
     *
     * @param stream the stream from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
  * @return 
     * @throws IOException if an error occurred while reading the stream
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     */
    public static Tex newTexture(InputStream stream, boolean mipmap, String fileSuffix) throws IOException, GLException {
        GL gl = GLContext.getCurrentGL();
        GLProfile glp = gl.getGLProfile();
        TexData data = newTexData(glp, stream, mipmap, fileSuffix);
        Tex texture = newTexture(gl, data);
        data.flush();
        return texture;
    }

    /**
     * Creates an OpenGL texture object from the specified URL using the
     * current OpenGL context.
     *
     * @param url the URL from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @throws IOException if an error occurred while reading the URL
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     */
    public static Tex newTexture(URL url, boolean mipmap, String fileSuffix) throws IOException, GLException {
        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(url.getPath());
        }
        GL gl = GLContext.getCurrentGL();
        GLProfile glp = gl.getGLProfile();
        TexData data = newTexData(glp, url, mipmap, fileSuffix);
        Tex texture = newTexture(gl, data);
        data.flush();
        return texture;
    }

    /**
     * Creates an OpenGL texture object associated with the given OpenGL
     * texture target. The texture has
     * no initial data. This is used, for example, to construct cube
     * maps out of multiple TexData objects.
     *
     * @param target the OpenGL target type, eg GL.GL_TEXTURE_2D,
     *               GL.GL_TEXTURE_RECTANGLE_ARB
     */
    public static Tex newTexture(int target) {
        return new Tex(target);
    }

    /**
     * Wraps an OpenGL texture ID from an external library and allows
     * some of the base methods from the Texture class, such as
     * binding and querying of texture coordinates, to be used with
     * it. Attempts to update such textures' contents will yield
     * undefined results.
     *
     * @param textureID the OpenGL texture object to wrap
     * @param target the OpenGL texture target, eg GL.GL_TEXTURE_2D,
     *               GL2.GL_TEXTURE_RECTANGLE
     * @param texWidth the width of the texture in pixels
     * @param texHeight the height of the texture in pixels
     * @param imgWidth the width of the image within the texture in
     *          pixels (if the content is a sub-rectangle in the upper
     *          left corner); otherwise, pass in texWidth
     * @param imgHeight the height of the image within the texture in
     *          pixels (if the content is a sub-rectangle in the upper
     *          left corner); otherwise, pass in texHeight
     * @param mustFlipVertically indicates whether the texture
     *                           coordinates must be flipped vertically
     *                           in order to properly display the
     *                           texture
     */
    public static Tex newTexture(int textureID,
                     int target,
                     int texWidth,
                     int texHeight,
                     int imgWidth,
                     int imgHeight,
                     boolean mustFlipVertically) {
    return new Tex(textureID,
               target,
               texWidth,
               texHeight,
               imgWidth,
               imgHeight,
               mustFlipVertically);
    }

    /**
     * Writes the given texture to a file. The type of the file is
     * inferred from its suffix. An OpenGL context must be current in
     * order to fetch the texture data back from the OpenGL pipeline.
     * This method causes the specified Texture to be bound to the
     * GL_TEXTURE_2D state. If no suitable writer for the requested file
     * format was found, throws an IOException. <P>
     *
     * Reasonable attempts are made to produce good results in the
     * resulting images. The Targa, SGI and ImageIO writers produce
     * results in the correct vertical orientation for those file
     * formats. The DDS writer performs no vertical flip of the data,
     * even in uncompressed mode. (It is impossible to perform such a
     * vertical flip with compressed data.) Applications should keep
     * this in mind when using this routine to save textures to disk for
     * later re-loading. <P>
     *
     * Any mipmaps for the specified texture are currently discarded
     * when it is written to disk, regardless of whether the underlying
     * file format supports multiple mipmaps in a given file.
     *
     * @throws IOException if an error occurred during writing or no
     *   suitable writer was found
     * @throws GLException if no OpenGL context was current or an
     *   OpenGL-related error occurred
     */
    public static void write(Tex texture, File file) throws IOException, GLException {
        if (texture.getTarget() != GL.GL_TEXTURE_2D) {
            throw new GLException("Only GL_TEXTURE_2D textures are supported");
        }

        // First fetch the texture data
        GL _gl = GLContext.getCurrentGL();
        if (!_gl.isGL2GL3()) {
            throw new GLException("Implementation only supports GL2GL3 (Use GLReadBufferUtil and the TexData variant), have: " + _gl);
        }
        GL2GL3 gl = _gl.getGL2();

        texture.bind(gl);
        int internalFormat = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2.GL_TEXTURE_INTERNAL_FORMAT);
        int width  = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2.GL_TEXTURE_WIDTH);
        int height = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2.GL_TEXTURE_HEIGHT);
        int border = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2.GL_TEXTURE_BORDER);
        TexData data = null;
        if (internalFormat == GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT ||
            internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT ||
            internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT ||
            internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT) {
            // Fetch using glGetCompressedTexImage
            int size   = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2.GL_TEXTURE_COMPRESSED_IMAGE_SIZE);
            ByteBuffer res = ByteBuffer.allocate(size);
            gl.glGetCompressedTexImage(GL.GL_TEXTURE_2D, 0, res);
            data = new TexData(gl.getGLProfile(), internalFormat, width, height, border, internalFormat, GL.GL_UNSIGNED_BYTE,
                                   false, true, true, res, null);
        } else {
            int bytesPerPixel = 0;
            int fetchedFormat = 0;
            switch (internalFormat) {
            case GL.GL_RGB:
            case GL2.GL_BGR:
            case GL.GL_RGB8:
                bytesPerPixel = 3;
                fetchedFormat = GL.GL_RGB;
                break;
            case GL.GL_RGBA:
            case GL.GL_BGRA:
            case GL2.GL_ABGR_EXT:
            case GL.GL_RGBA8:
                bytesPerPixel = 4;
                fetchedFormat = GL.GL_RGBA;
                break;
            default:
                throw new IOException("Unsupported texture internal format 0x" + Integer.toHexString(internalFormat));
            }

            // Fetch using glGetTexImage
            int packAlignment  = glGetInteger(gl, GL.GL_PACK_ALIGNMENT);
            int packRowLength  = glGetInteger(gl, GL2.GL_PACK_ROW_LENGTH);
            int packSkipRows   = glGetInteger(gl, GL2.GL_PACK_SKIP_ROWS);
            int packSkipPixels = glGetInteger(gl, GL2.GL_PACK_SKIP_PIXELS);
            int packSwapBytes  = glGetInteger(gl, GL2.GL_PACK_SWAP_BYTES);

            gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
            gl.glPixelStorei(GL2.GL_PACK_ROW_LENGTH, 0);
            gl.glPixelStorei(GL2.GL_PACK_SKIP_ROWS, 0);
            gl.glPixelStorei(GL2.GL_PACK_SKIP_PIXELS, 0);
            gl.glPixelStorei(GL2.GL_PACK_SWAP_BYTES, 0);

            ByteBuffer res = ByteBuffer.allocate((width + (2 * border)) *
                                                 (height + (2 * border)) *
                                                 bytesPerPixel);
            if (DEBUG) {
                System.out.println("Allocated buffer of size " + res.remaining() + " for fetched image (" +
                                   ((fetchedFormat == GL.GL_RGB) ? "GL_RGB" : "GL_RGBA") + ")");
            }
            gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, fetchedFormat, GL.GL_UNSIGNED_BYTE, res);

            gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, packAlignment);
            gl.glPixelStorei(GL2.GL_PACK_ROW_LENGTH, packRowLength);
            gl.glPixelStorei(GL2.GL_PACK_SKIP_ROWS, packSkipRows);
            gl.glPixelStorei(GL2.GL_PACK_SKIP_PIXELS, packSkipPixels);
            gl.glPixelStorei(GL2.GL_PACK_SWAP_BYTES, packSwapBytes);

            data = new TexData(gl.getGLProfile(), internalFormat, width, height, border, fetchedFormat, GL.GL_UNSIGNED_BYTE,
                                   false, false, false, res, null);

            if (DEBUG) {
                System.out.println("data.getPixelFormat() = " +
                                   ((data.getPixelFormat() == GL.GL_RGB) ? "GL_RGB" : "GL_RGBA"));
            }
        }

        write(data, file);
    }

    public static void write(TexData data, File file) throws IOException, GLException {
        for (Iterator<TexWriter> iter = textureWriters.iterator(); iter.hasNext(); ) {
            TexWriter writer = iter.next();
            if (writer.write(file, data)) {
                return;
            }
        }

        throw new IOException("No suitable texture writer found for "+file.getAbsolutePath());
    }

    //----------------------------------------------------------------------
    // SPI support
    //

    /**
     * Adds a TextureProvider to support reading of a new file format.
     * <p>
     * The last provider added, will be the first provider to be tested.
     * </p>
     */
    public static void addTexProvider(TexProvider provider) {
        // Must always add at the front so the ImageIO provider is last,
        // so we don't accidentally use it instead of a user's possibly
        // more optimal provider
        textureProviders.add(0, provider);
    }

    /**
     * Adds a TextureWriter to support writing of a new file format.
     * <p>
     * The last provider added, will be the first provider to be tested.
     * </p>
     */
    public static void addTexWriter(TexWriter writer) {
        // Must always add at the front so the ImageIO writer is last,
        // so we don't accidentally use it instead of a user's possibly
        // more optimal writer
        textureWriters.add(0, writer);
    }

    //---------------------------------------------------------------------------
    // Global disabling of texture rectangle extension
    //

    /** Toggles the use of the GL_ARB_texture_rectangle extension by the
        TextureIO classes. By default, on hardware supporting this
        extension, the TextureIO classes may use the
        GL_ARB_texture_rectangle extension for non-power-of-two
        textures. (If the hardware supports the
        GL_ARB_texture_non_power_of_two extension, that one is
        preferred.) In some situations, for example when writing
        shaders, it is advantageous to force the texture target to
        always be GL_TEXTURE_2D in order to have one version of the
        shader, even at the expense of texture memory in the case where
        NPOT textures are not supported. This method allows the use of
        the GL_ARB_texture_rectangle extension to be turned off globally
        for this purpose. The default is that the use of the extension
        is enabled. */
    public static void setTexRectEnabled(boolean enabled) {
        texRectEnabled = enabled;
    }

    /** Indicates whether the GL_ARB_texture_rectangle extension is
        allowed to be used for non-power-of-two textures; see {@link
        #setTexRectEnabled setTexRectEnabled}. */
    public static boolean isTexRectEnabled() {
        return texRectEnabled;
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    private static List<TexProvider> textureProviders = new ArrayList<TexProvider>();
    private static List<TexWriter>   textureWriters   = new ArrayList<TexWriter>();

    static {
        // ImageIO provider, the fall-back, must be the first one added
        if(GLProfile.isAWTAvailable()) {
            try {
                // Use reflection to avoid compile-time dependencies on AWT-related classes
                TexProvider provider = (TexProvider)
                    Class.forName("com.jogamp.opengl.util.texture.spi.awt.IIOTextureProvider").newInstance();
                addTexProvider(provider);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        // Other special-case providers
        addTexProvider(new DDSTexProvider());
        addTexProvider(new SGITexProvider());
        addTexProvider(new TGATexProvider());
        addTexProvider(new JPGTexProvider());
        addTexProvider(new PNGTexProvider());

        // ImageIO writer, the fall-back, must be the first one added
        if(GLProfile.isAWTAvailable()) {
            try {
                // Use reflection to avoid compile-time dependencies on AWT-related classes
                TexWriter writer = (TexWriter)
                    Class.forName("com.jogamp.opengl.util.texture.spi.awt.IIOTextureWriter").newInstance();
                addTexWriter(writer);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (Error e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        // Other special-case writers
        addTexWriter(new DDSTexWriter());
        addTexWriter(new SGITexWriter());
        addTexWriter(new TGATexWriter());
        addTexWriter(new NetPbmTexWriter());
        addTexWriter(new PNGTexWriter());
    }

    // Implementation methods
    private static TexData newTexDataImpl(GLProfile glp, File file,
                                                  int internalFormat,
                                                  int pixelFormat,
                                                  boolean mipmap,
                                                  String fileSuffix) throws IOException {
        if (file == null) {
            throw new IOException("File was null");
        }

        fileSuffix = toLowerCase(fileSuffix);

        for (Iterator<TexProvider> iter = textureProviders.iterator(); iter.hasNext(); ) {
            TexProvider provider = iter.next();
            TexData data = provider.newTexData(glp, file,
                                                       internalFormat,
                                                       pixelFormat,
                                                       mipmap,
                                                       fileSuffix);
            if (data != null) {
                return data;
            }
        }

        throw new IOException("No suitable reader for given file "+file.getAbsolutePath());
    }

    private static TexData newTexDataImpl(GLProfile glp, InputStream stream,
                                                  int internalFormat,
                                                  int pixelFormat,
                                                  boolean mipmap,
                                                  String fileSuffix) throws IOException {
        if (stream == null) {
            throw new IOException("Stream was null");
        }

        fileSuffix = toLowerCase(fileSuffix);

        // Note: use of BufferedInputStream works around 4764639/4892246
        if (!(stream instanceof BufferedInputStream)) {
            stream = new BufferedInputStream(stream);
        }

        for (Iterator<TexProvider> iter = textureProviders.iterator(); iter.hasNext(); ) {
            TexProvider provider = iter.next();
            TexData data = provider.newTexData(glp, stream,
                                                       internalFormat,
                                                       pixelFormat,
                                                       mipmap,
                                                       fileSuffix);
            if (data != null) {
                return data;
            }
        }

        throw new IOException("No suitable reader for given stream");
    }

    private static TexData newTexDataImpl(GLProfile glp, URL url,
                                                  int internalFormat,
                                                  int pixelFormat,
                                                  boolean mipmap,
                                                  String fileSuffix) throws IOException {
        if (url == null) {
            throw new IOException("URL was null");
        }

        fileSuffix = toLowerCase(fileSuffix);

        for (Iterator<TexProvider> iter = textureProviders.iterator(); iter.hasNext(); ) {
            TexProvider provider = iter.next();
            TexData data = provider.newTexData(glp, url,
                                                       internalFormat,
                                                       pixelFormat,
                                                       mipmap,
                                                       fileSuffix);
            if (data != null) {
                return data;
            }
        }

        throw new IOException("No suitable reader for given URL "+url);
    }

    //----------------------------------------------------------------------
    // DDS provider -- supports files only for now
    static class DDSTexProvider implements TexProvider {
        @Override
        public TexData newTexData(GLProfile glp, File file,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            if (DDS.equals(fileSuffix) ||
                DDS.equals(IOUtil.getFileSuffix(file))) {
                DDSImage image = DDSImage.read(file);
                return newTexData(glp, image, internalFormat, pixelFormat, mipmap);
            }

            return null;
        }

        @Override
        public TexData newTexData(GLProfile glp, InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            if (DDS.equals(fileSuffix) ||
                DDSImage.isDDSImage(stream)) {
                byte[] data = IOUtil.copyStream2ByteArray(stream);
                ByteBuffer buf = ByteBuffer.wrap(data);
                DDSImage image = DDSImage.read(buf);
                return newTexData(glp, image, internalFormat, pixelFormat, mipmap);
            }

            return null;
        }

        @Override
        public TexData newTexData(GLProfile glp, URL url,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            InputStream stream = new BufferedInputStream(url.openStream());
            try {
                return newTexData(glp, stream, internalFormat, pixelFormat, mipmap, fileSuffix);
            } finally {
                stream.close();
            }
        }

        private TexData newTexData(GLProfile glp, final DDSImage image,
                                           int internalFormat,
                                           int pixelFormat,
                                           boolean mipmap) {
            DDSImage.ImageInfo info = image.getMipMap(0);
            if (pixelFormat == 0) {
                switch (image.getPixelFormat()) {
                case DDSImage.D3DFMT_R8G8B8:
                    pixelFormat = GL.GL_RGB;
                    break;
                default:
                    pixelFormat = GL.GL_RGBA;
                    break;
                }
            }
            if (info.isCompressed()) {
                switch (info.getCompressionFormat()) {
                case DDSImage.D3DFMT_DXT1:
                    internalFormat = GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
                    break;
                case DDSImage.D3DFMT_DXT3:
                    internalFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                    break;
                case DDSImage.D3DFMT_DXT5:
                    internalFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                    break;
                default:
                    throw new RuntimeException("Unsupported DDS compression format \"" +
                                               DDSImage.getCompressionFormatName(info.getCompressionFormat()) + "\"");
                }
            }
            if (internalFormat == 0) {
                switch (image.getPixelFormat()) {
                case DDSImage.D3DFMT_R8G8B8:
                    pixelFormat = GL.GL_RGB;
                    break;
                default:
                    pixelFormat = GL.GL_RGBA;
                    break;
                }
            }
            TexData.Flusher flusher = new TexData.Flusher() {
                    @Override
                    public void flush() {
                        image.close();
                    }
                };
            TexData data;
            if (mipmap && image.getNumMipMaps() > 0) {
                Buffer[] mipmapData = new Buffer[image.getNumMipMaps()];
                for (int i = 0; i < image.getNumMipMaps(); i++) {
                    mipmapData[i] = image.getMipMap(i).getData();
                }
                data = new TexData(glp, internalFormat,
                                       info.getWidth(),
                                       info.getHeight(),
                                       0,
                                       pixelFormat,
                                       GL.GL_UNSIGNED_BYTE,
                                       info.isCompressed(),
                                       true,
                                       mipmapData,
                                       flusher);
            } else {
                // Fix this up for the end user because we can't generate
                // mipmaps for compressed textures
                mipmap = false;
                data = new TexData(glp, internalFormat,
                                       info.getWidth(),
                                       info.getHeight(),
                                       0,
                                       pixelFormat,
                                       GL.GL_UNSIGNED_BYTE,
                                       mipmap,
                                       info.isCompressed(),
                                       true,
                                       info.getData(),
                                       flusher);
            }
            return data;
        }
    }

    //----------------------------------------------------------------------
    // Base class for SGI RGB and TGA image providers
    static abstract class StreamBasedTexProvider implements TexProvider {
        @Override
        public TexData newTexData(GLProfile glp, File file,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            InputStream inStream = new BufferedInputStream(new FileInputStream(file));
            try {
                // The SGIImage and TGAImage implementations use InputStreams
                // anyway so there isn't much point in having a separate code
                // path for files
                return newTexData(glp, inStream,
                                      internalFormat,
                                      pixelFormat,
                                      mipmap,
                                      ((fileSuffix != null) ? fileSuffix : IOUtil.getFileSuffix(file)));
            } finally {
                inStream.close();
            }
        }

        @Override
        public TexData newTexData(GLProfile glp, URL url,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            InputStream stream = new BufferedInputStream(url.openStream());
            try {
                return newTexData(glp, stream, internalFormat, pixelFormat, mipmap, fileSuffix);
            } finally {
                stream.close();
            }
        }
    }

    //----------------------------------------------------------------------
    // SGI RGB image provider
    static class SGITexProvider extends StreamBasedTexProvider {
        @Override
        public TexData newTexData(GLProfile glp, InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            if (SGI.equals(fileSuffix) ||
                SGI_RGB.equals(fileSuffix) ||
                SGIImage.isSGIImage(stream)) {
                SGIImage image = SGIImage.read(stream);
                if (pixelFormat == 0) {
                    pixelFormat = image.getFormat();
                }
                if (internalFormat == 0) {
                    internalFormat = image.getFormat();
                }
                return new TexData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       pixelFormat,
                                       GL.GL_UNSIGNED_BYTE,
                                       mipmap,
                                       false,
                                       false,
                                       ByteBuffer.wrap(image.getData()),
                                       null);
            }

            return null;
        }
    }

    //----------------------------------------------------------------------
    // TGA (Targa) image provider
    static class TGATexProvider extends StreamBasedTexProvider {
        @Override
        public TexData newTexData(GLProfile glp, InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            if (TGA.equals(fileSuffix)) {
                TGAImage image = TGAImage.read(glp, stream);
                if (pixelFormat == 0) {
                    pixelFormat = image.getGLFormat();
                }
                if (internalFormat == 0) {
                    if(glp.isGL2ES3()) {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA8:GL.GL_RGB8;
                    } else {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
                    }
                }
                return new TexData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       pixelFormat,
                                       GL.GL_UNSIGNED_BYTE,
                                       mipmap,
                                       false,
                                       false,
                                       image.getData(),
                                       null);
            }

            return null;
        }
    }

    //----------------------------------------------------------------------
    // PNG image provider
    static class PNGTexProvider extends StreamBasedTexProvider {
        @Override
        public TexData newTexData(GLProfile glp, InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            if (PNG.equals(fileSuffix)) {
                PNGImage image = PNGImage.read(/*glp, */ stream);
                if (pixelFormat == 0) {
                    pixelFormat = image.getGLFormat();
                }
                if (internalFormat == 0) {
                    if(glp.isGL2ES3()) {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA8:GL.GL_RGB8;
                    } else {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
                    }
                }
                return new TexData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       pixelFormat,
                                       image.getGLType(),
                                       mipmap,
                                       false,
                                       false,
                                       image.getData(),
                                       null);
            }

            return null;
        }
    }

    //----------------------------------------------------------------------
    // JPEG image provider
    static class JPGTexProvider extends StreamBasedTexProvider {
        @Override
        public TexData newTexData(GLProfile glp, InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          boolean mipmap,
                                          String fileSuffix) throws IOException {
            if (JPG.equals(fileSuffix)) {
                JPEGImage image = JPEGImage.read(/*glp, */ stream);
                if (pixelFormat == 0) {
                    pixelFormat = image.getGLFormat();
                }
                if (internalFormat == 0) {
                    if(glp.isGL2ES3()) {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA8:GL.GL_RGB8;
                    } else {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
                    }
                }
                return new TexData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       pixelFormat,
                                       image.getGLType(),
                                       mipmap,
                                       false,
                                       false,
                                       image.getData(),
                                       null);
            }

            return null;
        }
    }

    //----------------------------------------------------------------------
    // DDS texture writer
    //
    static class DDSTexWriter implements TexWriter {
        @Override
        public boolean write(File file,
                             TexData data) throws IOException {
            if (DDS.equals(IOUtil.getFileSuffix(file))) {
                // See whether the DDS writer can handle this TexData
                final GLPixelAttributes pixelAttribs = data.getPixelAttributes();
                final int pixelFormat = pixelAttribs.format;
                final int pixelType   = pixelAttribs.type;
                if (pixelType != GL.GL_BYTE &&
                    pixelType != GL.GL_UNSIGNED_BYTE) {
                    throw new IOException("DDS writer only supports byte / unsigned byte textures");
                }

                int d3dFormat = 0;
                // FIXME: some of these are probably not completely correct and would require swizzling
                switch (pixelFormat) {
                    case GL.GL_RGB:                        d3dFormat = DDSImage.D3DFMT_R8G8B8; break;
                    case GL.GL_RGBA:                       d3dFormat = DDSImage.D3DFMT_A8R8G8B8; break;
                    case GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT:  d3dFormat = DDSImage.D3DFMT_DXT1; break;
                    case GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT: throw new IOException("RGBA DXT1 not yet supported");
                    case GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT: d3dFormat = DDSImage.D3DFMT_DXT3; break;
                    case GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT: d3dFormat = DDSImage.D3DFMT_DXT5; break;
                    default: throw new IOException("Unsupported pixel format 0x" + Integer.toHexString(pixelFormat) + " by DDS writer");
                }

                ByteBuffer[] mipmaps = null;
                if (data.getMipmapData() != null) {
                    mipmaps = new ByteBuffer[data.getMipmapData().length];
                    for (int i = 0; i < mipmaps.length; i++) {
                        mipmaps[i] = (ByteBuffer) data.getMipmapData()[i];
                    }
                } else {
                    mipmaps = new ByteBuffer[] { (ByteBuffer) data.getBuffer() };
                }

                DDSImage image = DDSImage.createFromData(d3dFormat,
                                                         data.getWidth(),
                                                         data.getHeight(),
                                                         mipmaps);
                image.write(file);
                return true;
            }

            return false;
        }
    }

    //----------------------------------------------------------------------
    // SGI (rgb) texture writer
    //
    static class SGITexWriter implements TexWriter {
        @Override
        public boolean write(File file,
                             TexData data) throws IOException {
            String fileSuffix = IOUtil.getFileSuffix(file);
            if (SGI.equals(fileSuffix) ||
                SGI_RGB.equals(fileSuffix)) {
                // See whether the SGI writer can handle this TexData
                final GLPixelAttributes pixelAttribs = data.getPixelAttributes();
                final int pixelFormat = pixelAttribs.format;
                final int pixelType   = pixelAttribs.type;
                if ((pixelFormat == GL.GL_RGB ||
                     pixelFormat == GL.GL_RGBA) &&
                    (pixelType == GL.GL_BYTE ||
                     pixelType == GL.GL_UNSIGNED_BYTE)) {
                    ByteBuffer buf = ((data.getBuffer() != null) ?
                                      (ByteBuffer) data.getBuffer() :
                                      (ByteBuffer) data.getMipmapData()[0]);
                    byte[] bytes;
                    if (buf.hasArray()) {
                        bytes = buf.array();
                    } else {
                        buf.rewind();
                        bytes = new byte[buf.remaining()];
                        buf.get(bytes);
                        buf.rewind();
                    }

                    SGIImage image = SGIImage.createFromData(data.getWidth(),
                                                             data.getHeight(),
                                                             (pixelFormat == GL.GL_RGBA),
                                                             bytes);
                    image.write(file, false);
                    return true;
                }

                throw new IOException("SGI writer doesn't support this pixel format / type (only GL_RGB/A + bytes)");
            }

            return false;
        }
    }

    //----------------------------------------------------------------------
    // TGA (Targa) texture writer

    static class TGATexWriter implements TexWriter {
        @Override
        public boolean write(File file,
                             TexData data) throws IOException {
            if (TGA.equals(IOUtil.getFileSuffix(file))) {
                // See whether the TGA writer can handle this TexData
                final GLPixelAttributes pixelAttribs = data.getPixelAttributes();
                final int pixelFormat = pixelAttribs.format;
                final int pixelType   = pixelAttribs.type;
                if ((pixelFormat == GL.GL_RGB ||
                     pixelFormat == GL.GL_RGBA ||
                     pixelFormat == GL2.GL_BGR ||
                     pixelFormat == GL.GL_BGRA ) &&
                    (pixelType == GL.GL_BYTE ||
                     pixelType == GL.GL_UNSIGNED_BYTE)) {

                    ByteBuffer buf = (ByteBuffer) data.getBuffer();
                    if (null == buf) {
                        buf = (ByteBuffer) data.getMipmapData()[0];
                    }
                    buf.rewind();

                    if( pixelFormat == GL.GL_RGB || pixelFormat == GL.GL_RGBA ) {
                        // Must reverse order of red and blue channels to get correct results
                        int skip = ((pixelFormat == GL.GL_RGB) ? 3 : 4);
                        for (int i = 0; i < buf.remaining(); i += skip) {
                            byte red  = buf.get(i + 0);
                            byte blue = buf.get(i + 2);
                            buf.put(i + 0, blue);
                            buf.put(i + 2, red);
                        }
                    }

                    TGAImage image = TGAImage.createFromData(data.getWidth(),
                                                             data.getHeight(),
                                                             (pixelFormat == GL.GL_RGBA || pixelFormat == GL.GL_BGRA),
                                                             false, buf);
                    image.write(file);
                    return true;
                }
                throw new IOException("TGA writer doesn't support this pixel format 0x"+Integer.toHexString(pixelFormat)+
                                      " / type 0x"+Integer.toHexString(pixelFormat)+" (only GL_RGB/A, GL_BGR/A + bytes)");
            }

            return false;
        }
    }

    //----------------------------------------------------------------------
    // PNG texture writer

    static class PNGTexWriter implements TexWriter {
        @Override
        public boolean write(File file, TexData data) throws IOException {
            if (PNG.equals(IOUtil.getFileSuffix(file))) {
                // See whether the PNG writer can handle this TexData
                final GLPixelAttributes pixelAttribs = data.getPixelAttributes();
                final int pixelFormat = pixelAttribs.format;
                final int pixelType   = pixelAttribs.type;
                final int bytesPerPixel = pixelAttribs.bytesPerPixel;
                final boolean reversedChannels;
                switch(pixelFormat) {
                    case GL.GL_ALPHA:
                    case GL.GL_LUMINANCE:
                    case GL2ES2.GL_RED:
                        reversedChannels=false;
                        break;
                    case GL.GL_RGB:
                        reversedChannels=false;
                        break;
                    case GL.GL_RGBA:
                        reversedChannels=false;
                        break;
                    case GL2.GL_BGR:
                        reversedChannels=true;
                        break;
                    case GL.GL_BGRA:
                        reversedChannels=true;
                        break;
                    default:
                        reversedChannels=false;
                        break;
                }
                if ( ( 1 == bytesPerPixel || 3 == bytesPerPixel || 4 == bytesPerPixel) &&
                     ( pixelType == GL.GL_BYTE || pixelType == GL.GL_UNSIGNED_BYTE)) {
                    ByteBuffer buf = (ByteBuffer) data.getBuffer();
                    if (null == buf) {
                        buf = (ByteBuffer) data.getMipmapData()[0];
                    }
                    buf.rewind();

                    PNGImage image = PNGImage.createFromData(data.getWidth(), data.getHeight(), -1f, -1f,
                                                             bytesPerPixel, reversedChannels, !data.getMustFlipVertically(), buf);
                    image.write(file, true);
                    return true;
                }
                throw new IOException("PNG writer doesn't support this pixel format 0x"+Integer.toHexString(pixelFormat)+
                                      " / type 0x"+Integer.toHexString(pixelFormat)+" (only GL_RGB/A, GL_BGR/A + bytes)");
            }
            return false;
        }
    }

    //----------------------------------------------------------------------
    // Helper routines
    //

    private static int glGetInteger(GL gl, int pname) {
        int[] tmp = new int[1];
        gl.glGetIntegerv(pname, tmp, 0);
        return tmp[0];
    }

    private static int glGetTexLevelParameteri(GL2GL3 gl, int target, int level, int pname) {
        int[] tmp = new int[1];
        gl.glGetTexLevelParameteriv(target, 0, pname, tmp, 0);
        return tmp[0];
    }

    private static String toLowerCase(String arg) {
        if (arg == null) {
            return null;
        }

        return arg.toLowerCase();
    }
}
