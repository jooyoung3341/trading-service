package com.trading.service.controller;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class WebController {

	
	@RequestMapping(value="/", method=RequestMethod.GET)
	public String bybit(Model model) throws InvalidKeyException, NoSuchAlgorithmException {

		return "web/home";
	}
}
