package com.sky.takeout.config;
import com.sky.takeout.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.security.Principal;

@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
 private final JwtService jwt; public WebSocketConfig(JwtService jwt){this.jwt=jwt;}
 public void configureMessageBroker(MessageBrokerRegistry r){r.enableSimpleBroker("/topic");r.setApplicationDestinationPrefixes("/app");}
 public void registerStompEndpoints(StompEndpointRegistry r){r.addEndpoint("/ws").setAllowedOriginPatterns("*");}
 public void configureClientInboundChannel(ChannelRegistration registration){registration.interceptors(new ChannelInterceptor(){
  @Override public Message<?> preSend(Message<?> message, MessageChannel channel){StompHeaderAccessor a=StompHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);if(a==null)return message;
   if(StompCommand.CONNECT.equals(a.getCommand())){String h=a.getFirstNativeHeader("Authorization");if(h==null||!h.startsWith("Bearer "))throw new IllegalArgumentException("WebSocket token missing");Claims c=jwt.claims(h.substring(7));String role=c.get("employeeId")!=null?"admin":"user:"+((Number)c.get("userId")).longValue();a.setUser((Principal)()->role);}
   if(StompCommand.SUBSCRIBE.equals(a.getCommand())){Principal p=a.getUser();String d=a.getDestination();if(p==null||d==null||(!p.getName().equals("admin")&&!d.equals("/topic/user/"+p.getName().substring(5)))||(p.getName().equals("admin")&&!d.equals("/topic/admin")))throw new IllegalArgumentException("WebSocket subscription denied");}
   return message;
  }});}
}
