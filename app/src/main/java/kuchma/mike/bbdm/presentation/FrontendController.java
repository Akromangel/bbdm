package kuchma.mike.bbdm.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {
    @GetMapping({"/game", "/scripting-adventure"})
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}
