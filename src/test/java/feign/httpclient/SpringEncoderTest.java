package feign.httpclient;

import com.google.protobuf.InvalidProtocolBufferException;
import com.scienjus.test.Request;
import feign.RequestTemplate;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.netflix.feign.support.SpringEncoder;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class SpringEncoderTest {

     // a protobuf object with some content
    private Request request = Request.newBuilder()
             .setId(1000000)
             .setMsg("Erlang/OTP 最初是爱立信为开发电信设备系统设计的编程语言平台，" +
            "电信设备(路由器、接入网关、…)典型设计是通过背板连接主控板卡与多块业务板卡的分布式系统。")
             .build();

    @Test
    public void testProtobuf() throws IOException, URISyntaxException {
        // protobuf convert to request by feign and ProtobufHttpMessageConverter
        RequestTemplate requestTemplate = newRequestTemplate();
        newEncoder().encode(request, Request.class, requestTemplate);
        HttpEntity entity = toApacheHttpEntity(requestTemplate);
        byte[] bytes = read(entity.getContent(), (int) entity.getContentLength());

        // http request-body is different with original protobuf body
        Assert.assertNotEquals(bytes.length, request.toByteArray().length);
        try {
            Request copy = Request.parseFrom(bytes);
            Assert.fail("Expected an InvalidProtocolBufferException to be thrown");
        } catch (InvalidProtocolBufferException e) {
            // success
        }
    }

    @Test
    public void testProtobufWithoutCharset() throws IOException, URISyntaxException {
        // protobuf convert to request by feign and ProtobufHttpMessageConverter
        RequestTemplate requestTemplate = newRequestTemplate();
        newEncoder().encode(request, Request.class, requestTemplate);
        // reset charset to null
        requestTemplate.body(requestTemplate.body(), null);
        HttpEntity entity = toApacheHttpEntity(requestTemplate);
        byte[] bytes = read(entity.getContent(), (int) entity.getContentLength());

        Assert.assertArrayEquals(bytes, request.toByteArray());
        Request copy = Request.parseFrom(bytes);
        Assert.assertEquals(request, copy);
    }

    private SpringEncoder newEncoder() {
        ObjectFactory<HttpMessageConverters> converters = () -> new HttpMessageConverters(new ProtobufHttpMessageConverter());
        return new SpringEncoder(converters);
    }

    private RequestTemplate newRequestTemplate() {
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.method("POST");
        return requestTemplate;
    }

    private HttpEntity toApacheHttpEntity(RequestTemplate requestTemplate) throws UnsupportedEncodingException, URISyntaxException, MalformedURLException {
        HttpUriRequest httpUriRequest = new ApacheHttpClient().toHttpUriRequest(requestTemplate.request(), new feign.Request.Options());
        return  ((HttpEntityEnclosingRequestBase)httpUriRequest).getEntity();
    }

    private byte[] read(InputStream in, int length) throws IOException {
        byte[] bytes = new byte[length];
        in.read(bytes);
        return bytes;
    }
}
