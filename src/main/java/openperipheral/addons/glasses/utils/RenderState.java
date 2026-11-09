package openperipheral.addons.glasses.utils;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Tracks the GL state used by the glasses renderer and only emits calls when the desired state differs from the cached
 * one. The GL backend is injectable so the state machine can be tested without loading LWJGL natives.
 */
public class RenderState {

    interface GlBackend {

        void disable(int cap);

        void enable(int cap);

        void blendFunc(int src, int dst);

        void pointSize(float size);

        void lineWidth(float width);

        void color4ub(byte r, byte g, byte b, byte a);

        boolean isEnabled(int cap);

        int getInteger(int pname);

        float getFloat(int pname);

        void getFloat(int pname, FloatBuffer params);

        FloatBuffer createFloatBuffer(int size);
    }

    private static final GlBackend LWJGL = new GlBackend() {

        @Override
        public void disable(int cap) {
            GL11.glDisable(cap);
        }

        @Override
        public void enable(int cap) {
            GL11.glEnable(cap);
        }

        @Override
        public void blendFunc(int src, int dst) {
            GL11.glBlendFunc(src, dst);
        }

        @Override
        public void pointSize(float size) {
            GL11.glPointSize(size);
        }

        @Override
        public void lineWidth(float width) {
            GL11.glLineWidth(width);
        }

        @Override
        public void color4ub(byte r, byte g, byte b, byte a) {
            GL11.glColor4ub(r, g, b, a);
        }

        @Override
        public boolean isEnabled(int cap) {
            return GL11.glIsEnabled(cap);
        }

        @Override
        public int getInteger(int pname) {
            return GL11.glGetInteger(pname);
        }

        @Override
        public float getFloat(int pname) {
            return GL11.glGetFloat(pname);
        }

        @Override
        public void getFloat(int pname, FloatBuffer params) {
            GL11.glGetFloat(pname, params);
        }

        @Override
        public FloatBuffer createFloatBuffer(int size) {
            return BufferUtils.createFloatBuffer(size);
        }
    };

    private final GlBackend gl;

    private final FloatBuffer colors;

    private boolean lighting;

    private boolean alphaTest;

    private boolean depthTest;

    private boolean texture;

    private boolean blend;

    private int blendSrc;

    private int blendDst;

    private float pointSize;

    private float lineWidth;

    private boolean cullFace;

    /**
     * Sentinel: 0 is a valid packed color (transparent black) and so is every 32-bit value, so use a long that cannot
     * collide with an int result.
     */
    private long color = Long.MIN_VALUE;

    public RenderState() {
        this(LWJGL);
    }

    RenderState(GlBackend gl) {
        this.gl = gl;
        this.colors = gl.createFloatBuffer(16);
    }

    public void forceKnownState() {
        gl.disable(GL11.GL_LIGHTING);
        this.lighting = false;

        gl.disable(GL11.GL_ALPHA_TEST);
        this.alphaTest = false;

        gl.disable(GL11.GL_TEXTURE_2D);
        this.texture = false;

        gl.disable(GL11.GL_DEPTH_TEST);
        this.depthTest = false;

        gl.enable(GL11.GL_BLEND);
        this.blend = true;

        this.blendDst = GL11.GL_ONE_MINUS_SRC_ALPHA;
        this.blendSrc = GL11.GL_SRC_ALPHA;
        gl.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        gl.disable(GL11.GL_CULL_FACE);
        this.cullFace = false;

        gl.pointSize(1.0f);
        this.pointSize = 1.0f;

        gl.lineWidth(1.0f);
        this.lineWidth = 1.0f;

        // The current GL color is unknown here; force the next setColor() to write it.
        this.color = Long.MIN_VALUE;
    }

    public void readState() {
        this.lighting = gl.isEnabled(GL11.GL_LIGHTING);
        this.alphaTest = gl.isEnabled(GL11.GL_ALPHA_TEST);
        this.texture = gl.isEnabled(GL11.GL_TEXTURE_2D);
        this.depthTest = gl.isEnabled(GL11.GL_DEPTH_TEST);
        this.blend = gl.isEnabled(GL11.GL_BLEND);
        this.blendSrc = gl.getInteger(GL11.GL_BLEND_SRC);
        this.blendDst = gl.getInteger(GL11.GL_BLEND_DST);
        this.cullFace = gl.isEnabled(GL11.GL_CULL_FACE);
        this.lineWidth = gl.getFloat(GL11.GL_LINE_WIDTH);
        this.pointSize = gl.getFloat(GL11.GL_POINT_SIZE);

        gl.getFloat(GL11.GL_CURRENT_COLOR, colors);

        float r = colors.get();
        float g = colors.get();
        float b = colors.get();
        float a = colors.get();
        this.color = (((int) (255 * r) & 0xFF) << 24) + (((int) (255 * g) & 0xFF) << 16)
            + (((int) (255 * b) & 0xFF) << 8)
            + (((int) (255 * a) & 0xFF) << 0);
        colors.clear();
    }

    public void setupCommonRender() {
        disableLight();
        disableAlphaTest();
        disableDepthTest();
        enableBlending();
        setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        disableCullFace();
    }

    public void setupSolidRender() {
        setupCommonRender();
        disableTexture();
    }

    public void setupTexturedRender() {
        setupCommonRender();
        enableTexture();
    }

    public void setColor(int rgb, float opacity) {
        final float clampedOpacity = Math.max(0, Math.min(1, opacity));
        final byte scaledOpacity = (byte) (clampedOpacity * 255);
        final int packedRgb = rgb & 0x00FFFFFF;
        int newColor = packedRgb << 8 | (scaledOpacity & 0xFF);

        if (newColor != color) {
            final byte r = (byte) (packedRgb >> 16);
            final byte g = (byte) (packedRgb >> 8);
            final byte b = (byte) (packedRgb >> 0);

            gl.color4ub(r, g, b, scaledOpacity);
            this.color = newColor;
        }
    }

    public void setLineWidth(float lineWidth) {
        lineWidth = Math.max(0, lineWidth);
        if (lineWidth != this.lineWidth) {
            gl.lineWidth(lineWidth);
            this.lineWidth = lineWidth;
        }
    }

    public void setPointSize(final float pointSize) {
        final float clamped = Math.max(0, pointSize);
        if (clamped != this.pointSize) {
            gl.pointSize(clamped);
            this.pointSize = clamped;
        }
    }

    public void enableBlending() {
        if (!blend) {
            gl.enable(GL11.GL_BLEND);
            blend = true;
        }
    }

    public void setBlendFunc(int src, int dst) {
        if (dst != this.blendDst || src != this.blendSrc) {
            gl.blendFunc(src, dst);
            this.blendDst = dst;
            this.blendSrc = src;
        }
    }

    public void disableBlending() {
        if (blend) {
            gl.disable(GL11.GL_BLEND);
            blend = false;
        }
    }

    public void enableDepthTest() {
        if (!depthTest) {
            gl.enable(GL11.GL_DEPTH_TEST);
            depthTest = true;
        }
    }

    public void disableDepthTest() {
        if (depthTest) {
            gl.disable(GL11.GL_DEPTH_TEST);
            depthTest = false;
        }
    }

    public void enableTexture() {
        if (!texture) {
            gl.enable(GL11.GL_TEXTURE_2D);
            texture = true;
        }
    }

    public void disableTexture() {
        if (texture) {
            gl.disable(GL11.GL_TEXTURE_2D);
            texture = false;
        }
    }

    public void enableAlphaTest() {
        if (!alphaTest) {
            gl.enable(GL11.GL_ALPHA_TEST);
            alphaTest = true;
        }
    }

    public void disableAlphaTest() {
        if (alphaTest) {
            gl.disable(GL11.GL_ALPHA_TEST);
            alphaTest = false;
        }
    }

    public void enableLight() {
        if (!lighting) {
            gl.enable(GL11.GL_LIGHTING);
            lighting = true;
        }
    }

    public void disableLight() {
        if (lighting) {
            gl.disable(GL11.GL_LIGHTING);
            lighting = false;
        }
    }

    public void enableCullFace() {
        if (!cullFace) {
            gl.enable(GL11.GL_CULL_FACE);
            cullFace = true;
        }
    }

    public void disableCullFace() {
        if (cullFace) {
            gl.disable(GL11.GL_CULL_FACE);
            cullFace = false;
        }
    }
}
