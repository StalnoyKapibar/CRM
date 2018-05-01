package com.ewp.crm.controllers.rest;

import com.ewp.crm.component.util.VKUtil;
import com.ewp.crm.models.Client;
import com.ewp.crm.models.User;
import com.ewp.crm.service.interfaces.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/vkMessage")
@EnableAsync
public class VkontakteRestController {

	private static Logger logger = LoggerFactory.getLogger(ClientRestController.class);

	private final ClientService clientService;

	private final VKUtil vkUtil;

    @Autowired
	public VkontakteRestController(ClientService clientService, VKUtil vkUtil) {
		this.clientService = clientService;
		this.vkUtil = vkUtil;
	}

	@RequestMapping(value = "/sendMessageVK", method = RequestMethod.POST)
	public ResponseEntity<String> addComment(@RequestParam(name = "clientId") Long clientId,
	                                         @RequestParam(name = "messageVK") String messageVK) {
		User userFromSession = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (userFromSession != null) {
			Client client = clientService.getClientByID(clientId);
			vkUtil.sendMessageToClient(client,messageVK);
			return new ResponseEntity<>(HttpStatus.OK);
		} else {
			return  new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
	}
}
