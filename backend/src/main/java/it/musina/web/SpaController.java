package it.musina.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaController {
    @GetMapping({"/login", "/connection"})
    String app() { return "forward:/index.html"; }
}
