package dev.sc.apm.controller;

import dev.sc.apm.dto.CreditAgreementDto;
import dev.sc.apm.dto.CreditApplicationDto;
import dev.sc.apm.dto.CreditApplicationRequestDto;
import dev.sc.apm.dto.PageResponseDto;
import dev.sc.apm.service.APMService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/credit-application")
@RequiredArgsConstructor
public class APMController {

    private final APMService apmService;

    @PostMapping
    public CreditApplicationDto createCreditApplication(@RequestBody CreditApplicationRequestDto creditRequestDto) {
        return apmService.createCreditApplication(creditRequestDto);
    }

    @PatchMapping("/{creditApplicationId}/signing")
    public CreditAgreementDto signCreditAgreement(@PathVariable long creditApplicationId) {
        return apmService.signCreditAgreement(creditApplicationId);
    }

    @GetMapping("/list")
    public PageResponseDto<CreditApplicationDto> getCreditApplications(@RequestParam int page) {
        return apmService.getPageCreditApplications(page);
    }

    @GetMapping("/list-agreement")
    public PageResponseDto<CreditAgreementDto> getCreditAgreements(@RequestParam int page) {
        return apmService.getPageCreditAgreements(page);
    }
}
