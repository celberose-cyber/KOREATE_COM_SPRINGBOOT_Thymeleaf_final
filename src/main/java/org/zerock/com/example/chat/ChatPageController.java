package org.zerock.com.example.chat;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chat")
public class ChatPageController {

    @GetMapping
    public String chatPage() {
        return "chat/chat"; // templates/chat/chat.html
    }
}
