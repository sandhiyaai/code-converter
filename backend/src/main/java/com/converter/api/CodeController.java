package com.converter.api;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CodeController {

    private final CodeConvertService service;

    public CodeController(CodeConvertService service) {
        this.service = service;
    }

    @PostMapping("/convert")
    public Map<String, String> convert(@RequestBody Map<String, String> req) {

        String code = req.get("code");
        String from = req.get("sourceLanguage");
        String to = req.get("targetLanguage");

        String output = service.convert(from, to, code);

        Map<String, String> res = new HashMap<>();
        res.put("output", output);
        return res;
    }
}




