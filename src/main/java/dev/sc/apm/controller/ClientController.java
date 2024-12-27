package dev.sc.apm.controller;

import dev.sc.apm.dto.ClientDto;
import dev.sc.apm.dto.FindClientsRequestDto;
import dev.sc.apm.dto.PageResponseDto;
import dev.sc.apm.service.ClientService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/list")
    public PageResponseDto<ClientDto> findClients(
            @RequestParam @Positive int page,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String middleName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String passport) {

        FindClientsRequestDto requestDto = FindClientsRequestDto.builder()
                .firstName(firstName)
                .lastName(lastName)
                .middleName(middleName)
                .phone(phone)
                .passport(passport)
                .build();

        return clientService.findClients(page, requestDto);
    }
}
