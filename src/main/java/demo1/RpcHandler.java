package demo1;

import java.util.Map;

import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import demo.demo1.RpcRequest;
import demo.demo1.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 使用RpcHandler中处理 RPC 请求，只需扩展 Netty 的SimpleChannelInboundHandler抽象类即可.
 * FastClass与FastMethod。
 * @author Administrator
 *
 */
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
	    private static final Logger LOGGER = LoggerFactory.getLogger(RpcHandler.class);
	    private final Map<String, Object> handlerMap;
	    public RpcHandler(Map<String, Object> handlerMap) {
	    		this.handlerMap = handlerMap;
	    }
	
	    @Override
	    public void channelRead0(final ChannelHandlerContext ctx, RpcRequest request) throws Exception {
		        RpcResponse response = new RpcResponse();
		        response.setRequestId(request.getRequestId());
		        try {
			            Object result = handle(request);
			            response.setResult(result);
		        } catch (Throwable t) {
		        		response.setError(t);
		        }
		        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	    }
	
	    private Object handle(RpcRequest request) throws Throwable {
		        String className = request.getClassName();
		        Object serviceBean = handlerMap.get(className);
		
		        Class<?> serviceClass = serviceBean.getClass();
		        String methodName = request.getMethodName();
		        Class<?>[] parameterTypes = request.getParameterTypes();
		        Object[] parameters = request.getParameters();
		
		        /*Method method = serviceClass.getMethod(methodName, parameterTypes);
		        method.setAccessible(true);
		        return method.invoke(serviceBean, parameters);*/
		        
		        //为了避免使用 Java 反射带来的性能问题，我们可以使用 CGLib 提供的反射 API，如下面用到的
		        FastClass serviceFastClass = FastClass.create(serviceClass);
		        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
		        return serviceFastMethod.invoke(serviceBean, parameters);
	    }
	
	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		        LOGGER.error("server caught exception", cause);
		        ctx.close();
	    }
}