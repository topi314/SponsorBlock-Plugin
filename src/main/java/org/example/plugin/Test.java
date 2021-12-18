package org.example.plugin;

import dev.arbjerg.lavalink.api.ISocketContext;
import dev.arbjerg.lavalink.api.PluginEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Test implements PluginEventHandler {
    private static final Logger log = LoggerFactory.getLogger(Test.class);

    public void PluginEventHandler() {
        log.info("Hello, world!");
    }

    @Override
    public void onWebSocketOpen(ISocketContext context) {
        log.info("Websocket opened!");
    }
}
