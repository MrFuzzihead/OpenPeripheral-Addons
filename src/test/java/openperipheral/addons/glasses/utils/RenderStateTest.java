package openperipheral.addons.glasses.utils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class RenderStateTest {

    private static final int GL_BLEND = 3042;
    private static final int GL_CULL_FACE = 2884;
    private static final int GL_DEPTH_TEST = 2929;
    private static final int GL_LIGHTING = 2896;
    private static final int GL_ALPHA_TEST = 3008;
    private static final int GL_TEXTURE_2D = 3553;
    private static final int GL_SRC_ALPHA = 770;
    private static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    private static final int GL_ONE = 1;
    private static final int GL_ZERO = 0;
    private static final int GL_BLEND_SRC = 3041;
    private static final int GL_BLEND_DST = 3040;
    private static final int GL_LINE_WIDTH = 2848;
    private static final int GL_POINT_SIZE = 2833;
    private static final int GL_CURRENT_COLOR = 2816;

    private static final class FakeGl implements RenderState.GlBackend {

        private final List<String> calls = new ArrayList<String>();

        private boolean enabled;

        @Override
        public void disable(int cap) {
            calls.add("disable:" + cap);
        }

        @Override
        public void enable(int cap) {
            calls.add("enable:" + cap);
        }

        @Override
        public void blendFunc(int src, int dst) {
            calls.add("blend:" + src + ":" + dst);
        }

        @Override
        public void pointSize(float size) {
            calls.add("point:" + size);
        }

        @Override
        public void lineWidth(float width) {
            calls.add("line:" + width);
        }

        @Override
        public void color4ub(byte r, byte g, byte b, byte a) {
            calls.add("color:" + r + ":" + g + ":" + b + ":" + a);
        }

        @Override
        public boolean isEnabled(int cap) {
            return enabled;
        }

        @Override
        public int getInteger(int pname) {
            return pname == GL_BLEND_SRC ? 11 : 22;
        }

        @Override
        public float getFloat(int pname) {
            return pname == GL_LINE_WIDTH ? 2.5f : 3.5f;
        }

        @Override
        public void getFloat(int pname, FloatBuffer params) {
            params.put(1.0f)
                .put(0.5f)
                .put(0.25f)
                .put(1.0f)
                .flip();
        }

        @Override
        public FloatBuffer createFloatBuffer(int size) {
            return FloatBuffer.allocate(size);
        }
    }

    @Test
    public void forceKnownStateEstablishesADeterministicBaseline() {
        final FakeGl gl = new FakeGl();
        final RenderState state = new RenderState(gl);
        state.forceKnownState();

        Assert.assertTrue(gl.calls.contains("disable:" + GL_LIGHTING));
        Assert.assertTrue(gl.calls.contains("disable:" + GL_ALPHA_TEST));
        Assert.assertTrue(gl.calls.contains("disable:" + GL_TEXTURE_2D));
        Assert.assertTrue(gl.calls.contains("disable:" + GL_DEPTH_TEST));
        Assert.assertTrue(gl.calls.contains("enable:" + GL_BLEND));
        Assert.assertTrue(gl.calls.contains("blend:" + GL_SRC_ALPHA + ":" + GL_ONE_MINUS_SRC_ALPHA));
        Assert.assertTrue(gl.calls.contains("disable:" + GL_CULL_FACE));
        Assert.assertTrue(gl.calls.contains("point:1.0"));
        Assert.assertTrue(gl.calls.contains("line:1.0"));

        // The first color write must not be skipped just because the requested color is transparent black.
        state.setColor(0, 0);
        Assert.assertTrue(gl.calls.contains("color:0:0:0:0"));
    }

    @Test
    public void colorLinePointAndBlendStateAreCached() {
        final FakeGl gl = new FakeGl();
        final RenderState state = new RenderState(gl);
        state.forceKnownState();
        gl.calls.clear();

        state.setColor(0xFFFFFF, 1.0f);
        state.setColor(0xFFFFFF, 1.0f);
        Assert.assertEquals(1, count(gl.calls, "color:-1:-1:-1:-1"));

        state.setLineWidth(2.0f);
        state.setLineWidth(2.0f);
        Assert.assertEquals(1, count(gl.calls, "line:2.0"));

        state.setPointSize(3.0f);
        state.setPointSize(3.0f);
        Assert.assertEquals(1, count(gl.calls, "point:3.0"));

        state.disableBlending();
        gl.calls.clear();
        state.enableBlending();
        state.enableBlending();
        Assert.assertEquals(1, count(gl.calls, "enable:" + GL_BLEND));

        state.disableBlending();
        state.disableBlending();
        Assert.assertEquals(1, count(gl.calls, "disable:" + GL_BLEND));

        state.setBlendFunc(GL_ONE, GL_ZERO);
        state.setBlendFunc(GL_ONE, GL_ZERO);
        Assert.assertEquals(1, count(gl.calls, "blend:" + GL_ONE + ":" + GL_ZERO));
    }

    @Test
    public void capabilityTogglesAreIdempotent() {
        final FakeGl gl = new FakeGl();
        final RenderState state = new RenderState(gl);

        state.enableTexture();
        state.enableTexture();
        state.disableTexture();
        state.disableTexture();
        state.enableDepthTest();
        state.enableDepthTest();
        state.disableDepthTest();
        state.disableDepthTest();
        state.enableAlphaTest();
        state.enableAlphaTest();
        state.disableAlphaTest();
        state.disableAlphaTest();
        state.enableLight();
        state.enableLight();
        state.disableLight();
        state.disableLight();
        state.enableCullFace();
        state.enableCullFace();
        state.disableCullFace();
        state.disableCullFace();

        Assert.assertEquals(1, count(gl.calls, "enable:" + GL_TEXTURE_2D));
        Assert.assertEquals(1, count(gl.calls, "disable:" + GL_TEXTURE_2D));
        Assert.assertEquals(1, count(gl.calls, "enable:" + GL_DEPTH_TEST));
        Assert.assertEquals(1, count(gl.calls, "disable:" + GL_DEPTH_TEST));
        Assert.assertEquals(1, count(gl.calls, "enable:" + GL_ALPHA_TEST));
        Assert.assertEquals(1, count(gl.calls, "disable:" + GL_ALPHA_TEST));
        Assert.assertEquals(1, count(gl.calls, "enable:" + GL_LIGHTING));
        Assert.assertEquals(1, count(gl.calls, "disable:" + GL_LIGHTING));
        Assert.assertEquals(1, count(gl.calls, "enable:" + GL_CULL_FACE));
        Assert.assertEquals(1, count(gl.calls, "disable:" + GL_CULL_FACE));

        state.enableTexture();
        gl.calls.clear();
        state.setupSolidRender();
        Assert.assertTrue(gl.calls.contains("disable:" + GL_TEXTURE_2D));
        state.disableTexture();
        gl.calls.clear();
        state.setupTexturedRender();
        Assert.assertTrue(gl.calls.contains("enable:" + GL_TEXTURE_2D));
        state.disableBlending();
        gl.calls.clear();
        state.setupCommonRender();
        Assert.assertTrue(gl.calls.contains("enable:" + GL_BLEND));
    }

    @Test
    public void valuesAreClampedToSaneRanges() {
        final FakeGl gl = new FakeGl();
        final RenderState state = new RenderState(gl);
        state.forceKnownState();
        state.setColor(0xFFFFFF, -1);
        state.setLineWidth(-1);
        state.setPointSize(-1);

        Assert.assertTrue(gl.calls.contains("color:-1:-1:-1:0"));
        Assert.assertTrue(gl.calls.contains("line:0.0"));
        Assert.assertTrue(gl.calls.contains("point:0.0"));
    }

    @Test
    public void readStateCapturesCurrentGlValues() {
        final FakeGl gl = new FakeGl();
        gl.enabled = true;
        final RenderState state = new RenderState(gl);
        state.readState();

        // The fake current color is (1.0, 0.5, 0.25, 1.0) -> opaque orange. A subsequent identical setColor should
        // be suppressed because readState synchronized the cache.
        gl.calls.clear();
        state.setColor(0xFF7F3F, 1.0f);
        Assert.assertTrue(gl.calls.isEmpty());
    }

    private static int count(List<String> calls, String value) {
        int result = 0;
        for (String call : calls) if (call.equals(value)) result++;
        return result;
    }
}
