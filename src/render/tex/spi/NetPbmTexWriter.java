package render.tex.spi;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.texture.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

import javax.media.opengl.*;

import render.tex.TexData;

public class NetPbmTexWriter implements TexWriter {
    int magic;

    public NetPbmTexWriter() {
        this(0); // auto
    }

    /**
     * supported magic values are:<br>
     * <pre>
     *   magic 0 - detect by file suffix (TextureIO compliant)
     *   magic 6 - PPM binary RGB
     *   magic 7 - PAM binary RGB or RGBA
     * </pre>
     */
    public NetPbmTexWriter(int magic) {
        switch(magic) {
            case 0:
            case 6:
            case 7:
                break;
            default:
                throw new GLException("Unsupported magic: "+magic+", should be 0 (auto), 6 (PPM) or 7 (PAM)");
        }
        this.magic = magic;
    }

    public int getMagic() { return magic; }

    /** @see TextureIO#PPM */
    public static final String PPM     = TextureIO.PPM;
    /** @see TextureIO#PAM */
    public static final String PAM     = TextureIO.PAM;

    public String getSuffix() { return (magic==6)?PPM:PAM; }

    @Override
    public boolean write(File file, TexData data) throws IOException {
        boolean res;
        final int magic_old = magic;

        // file suffix selection
        if (0==magic) {
            if (PPM.equals(IOUtil.getFileSuffix(file))) {
                magic = 6;
            } else if (PAM.equals(IOUtil.getFileSuffix(file))) {
                magic = 7;
            } else {
                return false;
            }
        }
        try {
            res = writeImpl(file, data);
        } finally {
            magic = magic_old;
        }
        return res;
    }

    private boolean writeImpl(File file, TexData data) throws IOException {
        int pixelFormat = data.getPixelFormat();
        final int pixelType   = data.getPixelType();
        if ((pixelFormat == GL.GL_RGB ||
             pixelFormat == GL.GL_RGBA ||
             pixelFormat == GL2.GL_BGR ||
             pixelFormat == GL.GL_BGRA ) &&
            (pixelType == GL.GL_BYTE ||
             pixelType == GL.GL_UNSIGNED_BYTE)) {

            ByteBuffer buf = (ByteBuffer) data.getBuffer();
            if (null == buf ) {
                buf = (ByteBuffer) data.getMipmapData()[0];
            }
            buf.rewind();

            int comps = ( pixelFormat == GL.GL_RGBA || pixelFormat == GL.GL_BGRA ) ? 4 : 3 ;

            if( pixelFormat == GL2.GL_BGR || pixelFormat == GL.GL_BGRA ) {
                // Must reverse order of red and blue channels to get correct results
                for (int i = 0; i < buf.remaining(); i += comps) {
                    byte red  = buf.get(i + 0);
                    byte blue = buf.get(i + 2);
                    buf.put(i + 0, blue);
                    buf.put(i + 2, red);
                }
                pixelFormat = ( 4 == comps ) ? GL.GL_RGBA : GL.GL_RGB;
                data.setPixelFormat(pixelFormat);
            }

            if(magic==6 && comps==4) {
                throw new IOException("NetPbmTextureWriter magic 6 (PPM) doesn't RGBA pixel format, use magic 7 (PAM)");
            }

            FileOutputStream fos = IOUtil.getFileOutputStream(file, true);

            StringBuilder header = new StringBuilder();
            header.append("P");
            header.append(magic);
            header.append("\n");
            if(7==magic) {
                header.append("WIDTH ");
            }
            header.append(data.getWidth());
            if(7==magic) {
                header.append("\nHEIGHT ");
            } else {
                header.append(" ");
            }
            header.append(data.getHeight());
            if(7==magic) {
                header.append("\nDEPTH ");
                header.append(comps);
                header.append("\nMAXVAL 255\nTUPLTYPE ");
                if(pixelFormat == GL.GL_RGBA) {
                    header.append("RGB_ALPHA");
                } else {
                    header.append("RGB");
                }
                header.append("\nENDHDR\n");
            } else {
                header.append("\n255\n");
            }

            fos.write(header.toString().getBytes());

            FileChannel fosc = fos.getChannel();
            fosc.write(buf);
            fosc.force(true);
            fosc.close();
            fos.close();
            buf.rewind();

            return true;
        }
        throw new IOException("NetPbmTextureWriter writer doesn't support this pixel format / type (only GL_RGB/A + bytes)");
    }
}
