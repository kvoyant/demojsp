package com.example.demojsp.domain.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/board")
public class BoardController {

    @GetMapping("/list")
    public String listPage() {
        return "board/list";
    }

    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable("id") Long id, Model model) {
        model.addAttribute("boardId", id);
        return "board/detail";
    }

    @GetMapping("/form")
    public String formPage(Model model) {
        model.addAttribute("boardId", null);
        return "board/form";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable("id") Long id, Model model) {
        model.addAttribute("boardId", id);
        return "board/form";
    }
}
