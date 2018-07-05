package com.ewp.crm.component;

import com.ewp.crm.component.util.VKUtil;
import com.ewp.crm.component.util.interfaces.SMSUtil;
import com.ewp.crm.configs.inteface.VKConfig;
import com.ewp.crm.exceptions.parse.ParseClientException;
import com.ewp.crm.exceptions.util.VKAccessTokenException;
import com.ewp.crm.models.*;
import com.ewp.crm.service.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@EnableScheduling
public class ScheduleTasks {

	private final VKUtil vkUtil;

	private final ClientService clientService;

	private final StatusService statusService;

	private final SocialNetworkService socialNetworkService;

	private final SocialNetworkTypeService socialNetworkTypeService;

	private final SMSUtil smsUtil;

	private final SMSInfoService smsInfoService;

	private final SendNotificationService sendNotificationService;

	private final VkMemberService vkMemberService;

	private String firstContactMessage;

	private VKConfig vkConfig;

	private static Logger logger = LoggerFactory.getLogger(ScheduleTasks.class);

	@Autowired
	public ScheduleTasks(VKUtil vkUtil, ClientService clientService, StatusService statusService, SocialNetworkService socialNetworkService, SocialNetworkTypeService socialNetworkTypeService, SMSUtil smsUtil, SMSInfoService smsInfoService, SendNotificationService sendNotificationService, VkMemberService vkMemberService, VKConfig vkConfig) {
		this.vkUtil = vkUtil;
		this.clientService = clientService;
		this.statusService = statusService;
		this.socialNetworkService = socialNetworkService;
		this.socialNetworkTypeService = socialNetworkTypeService;
		this.smsUtil = smsUtil;
		this.smsInfoService = smsInfoService;
		this.sendNotificationService = sendNotificationService;
		this.vkMemberService = vkMemberService;
		this.vkConfig = vkConfig;
	}

	private void addClient(Client newClient) {
		Status newClientsStatus = statusService.getFirstStatusForClient();
		newClient.setStatus(newClientsStatus);
		newClient.setState(Client.State.NEW);
		newClient.getSocialNetworks().get(0).setSocialNetworkType(socialNetworkTypeService.getByTypeName("vk"));
		clientService.addClient(newClient);
		logger.info("New client with id{} has added from VK", newClient.getId());
	}

	private void updateClient(Client newClient) {
		SocialNetwork socialNetwork = newClient.getSocialNetworks().get(0);
		Client updateClient = socialNetworkService.getSocialNetworkByLink(socialNetwork.getLink()).getClient();
		updateClient.setPhoneNumber(newClient.getPhoneNumber());
		updateClient.setEmail(newClient.getEmail());
		updateClient.setAge(newClient.getAge());
		updateClient.setSex(newClient.getSex());
		clientService.updateClient(updateClient);
		logger.info("Client with id{} has updated from VK", updateClient.getId());
	}

	@Scheduled(fixedRate = 100_000)
	private void handleRequestsFromVk() {
		try {
			Optional<List<String>> newMassages = vkUtil.getNewMassages();
			if (newMassages.isPresent()) {
				for (String message : newMassages.get()) {
					try {
						Client newClient = vkUtil.parseClientFromMessage(message);
						SocialNetwork socialNetwork = newClient.getSocialNetworks().get(0);
						if (Optional.ofNullable(socialNetworkService.getSocialNetworkByLink(socialNetwork.getLink())).isPresent()) {
							updateClient(newClient);
						} else {
							addClient(newClient);
						}
					} catch (ParseClientException e) {
						logger.error(e.getMessage());
					}
				}
			}
		} catch (VKAccessTokenException ex) {
			logger.error(ex.getMessage());
		}
	}

	@Scheduled(fixedRate = 100_000)
	private void handleRequestsFromVkCommunityMessages() {
		Optional<List<Long>> newUsers = vkUtil.getUsersIdFromCommunityMessages();
		if (newUsers.isPresent()) {
			for (Long id : newUsers.get()) {
				Optional<Client> newClient = vkUtil.getClientFromVkId(id);
				if (newClient.isPresent()) {
					SocialNetwork socialNetwork = newClient.get().getSocialNetworks().get(0);
					if (!(Optional.ofNullable(socialNetworkService.getSocialNetworkByLink(socialNetwork.getLink())).isPresent())) {
						addClient(newClient.get());
					}
				}
			}
		}
	}

	@Scheduled(cron = "0 0 8 * * *")
	private void checkClientActivationDate() {
		for (Client client : clientService.getChangeActiveClients()) {
			client.setPostponeDate(null);
			clientService.updateClient(client);
		}
	}

	//будет отправлять сообщения для новых подписчиков группы.
	@Scheduled(fixedRate = 60_000)
	private void findNewMembersAndSendFirstMessage(){
		//TODO перебор всех групп в цикле
		ArrayList<VkMember> freshMemberList = vkUtil.getAllVKMembers(null, 0L).get();
		List<VkMember> lastMemberList = vkMemberService.getAll();
		for (VkMember vkMember : freshMemberList){
			if(!lastMemberList.contains(vkMember)){
				//vkUtil.sendMessageById(vkMember.getVkId(), vkConfig.getFirstContactMessage());
				vkMemberService.add(vkMember);
			}
		}

	}

	//TODO 600_0000 after tests
	@Scheduled(fixedRate = 6_000)
	private void checkSMSMessages() {
		logger.info("start checking sms statuses");
		List<SMSInfo> queueSMS = smsInfoService.getSMSbyDelivery(false);
		for (SMSInfo sms : queueSMS) {
			String status = smsUtil.getStatusMessage(sms.getSmsId());
			if (!status.equals("queued")) {
				if (status.equals("delivered")) {
					sms.setDelivered(true);
					smsInfoService.updateSMSInfo(sms);
				} else {
					String forView = determineStatusOfResponse(status);
					sendNotificationService.sendNotification(forView,sms.getClient(),sms.getUser());
					smsInfoService.deleteSMSInfo(sms.getId());
				}
			}
		}
	}

	//TODO не знаю куда запихнуть
	private String determineStatusOfResponse(String status) {
		String info;
		switch (status) {
			case "delivery error":
				info = "Номер заблокирован или вне зоны";
				break;
			case "incorrect id":
				info = "Неверный id сообщения";
				break;
			default:
				info = "Неизвестная ошибка";
		}
		return info;
	}
}