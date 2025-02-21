package com.example.final_project.api.controllers;

import com.example.final_project.api.requests.budgets.RegisterBudgetRequest;
import com.example.final_project.api.requests.budgets.UpdateBudgetRequest;
import com.example.final_project.api.responses.BudgetResponseDto;
import com.example.final_project.api.responses.BudgetStatusDTO;
import com.example.final_project.domain.budgets.Budget;
import com.example.final_project.domain.budgets.BudgetId;
import com.example.final_project.domain.budgets.BudgetService;
import com.example.final_project.domain.budgets.TypeOfBudget;
import com.example.final_project.domain.tokens.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.example.final_project.api.controllers.BudgetController.BUDGETS_BASE_PATH;

@RestController
@RequestMapping(BUDGETS_BASE_PATH)
public class BudgetController {

	static final String BUDGETS_BASE_PATH = "/budgets";

	@Value("${jwt.private.key}")
	private RSAPrivateKey priv;
	private final BudgetService budgetService;
	private final TokenService tokenService;

	public BudgetController(BudgetService budgetService, TokenService tokenService) {
		this.budgetService = budgetService;
		this.tokenService = tokenService;
	}

	@GetMapping("/{rawBudgetId}")
	ResponseEntity<BudgetResponseDto> getSingleBudget(
			@PathVariable String rawBudgetId, Authentication authentication
	) {
		String userId = tokenService.extractUserIdFromToken(authentication);
		Optional<Budget> budgetById = budgetService.getBudgetById(new BudgetId(rawBudgetId), userId);
		return ResponseEntity.of(budgetById.map(BudgetResponseDto::fromDomain));
	}

	@GetMapping
	ResponseEntity<Page<BudgetResponseDto>> getBudgetByPage(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size,
			@RequestParam(required = false, defaultValue = "budgetId") String sortBy,
			@RequestParam(required = false, defaultValue = "DESC") Sort.Direction sortDirection,
			Authentication authentication
	) {

		String userId = tokenService.extractUserIdFromToken(authentication);

		return ResponseEntity.ok(budgetService.findAllByPage(userId,
						PageRequest.of(page, size, Sort.by(sortDirection, sortBy)))
				.map(BudgetResponseDto::fromDomain));
	}

	@GetMapping("/status/{rawBudgetId}")
	ResponseEntity<BudgetStatusDTO> getBudgetStatus(@PathVariable String rawBudgetId, Authentication authentication) {
		String userId = tokenService.extractUserIdFromToken(authentication);

		return ResponseEntity.status(HttpStatus.OK).body(
				budgetService.getBudgetStatus(new BudgetId(rawBudgetId), userId)
		);
	}

	@PostMapping
	ResponseEntity<BudgetResponseDto> registerNewBudget(
			@RequestBody @Valid RegisterBudgetRequest request, Authentication authentication
	) {

		String userId = tokenService.extractUserIdFromToken(authentication);

		Budget newBudget = budgetService.registerNewBudget(request.title(), request.limit(),
				request.typeOfBudget(), request.maxSingleExpense(), userId);
		BudgetResponseDto budgetResponseDto = BudgetResponseDto.fromDomain(newBudget);
		return ResponseEntity.created(URI.create("/expenses/" + budgetResponseDto.budgetId())).body(budgetResponseDto);
	}

	@DeleteMapping("/{rawBudgetId}")
	public ResponseEntity<BudgetResponseDto> deleteBudget(@PathVariable String rawBudgetId, Authentication authentication) {
		String userId = tokenService.extractUserIdFromToken(authentication);
		budgetService.getBudgetById(new BudgetId(rawBudgetId), userId)
				.ifPresent(budget -> budgetService.deleteBudgetById(budget.budgetId(), userId));
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{rawBudgetId}")
	public ResponseEntity<BudgetResponseDto> updateBudget(@PathVariable UUID rawBudgetId, @RequestBody RegisterBudgetRequest request, Authentication authentication) {
		String userId = tokenService.extractUserIdFromToken(authentication);
		Budget updatedBudget = budgetService.updateBudgetById(new BudgetId(
						rawBudgetId.toString()),
				request.title(),
				request.limit(),
				request.typeOfBudget(),
				request.maxSingleExpense(),
				userId,
				LocalDateTime.now()
		);
		return ResponseEntity.ok(BudgetResponseDto.fromDomain(updatedBudget));
	}

	@PatchMapping("/{rawBudgetId}")
	public ResponseEntity<BudgetResponseDto> updateBudgetField(@PathVariable String rawBudgetId,
															   @RequestBody UpdateBudgetRequest request,
															   Authentication authentication) {

		String userId = tokenService.extractUserIdFromToken(authentication);

		Optional<String> title = Optional.ofNullable(request.title());
		Optional<BigDecimal> limit = Optional.ofNullable(request.limit());
		Optional<TypeOfBudget> typeOfBudget = Optional.ofNullable(request.typeOfBudget());
		Optional<BigDecimal> maxSingleExpense = Optional.ofNullable(request.maxSingleExpense());
		Optional<LocalDateTime> timestamp = Optional.empty();

		return ResponseEntity.of(budgetService.updateBudgetContent(new BudgetId(rawBudgetId), title,
						limit,
						typeOfBudget,
						maxSingleExpense,
						userId,
						timestamp
				)
				.map(BudgetResponseDto::fromDomain));
	}

	@GetMapping("/{budgetId}/status")
	ResponseEntity<BudgetStatusDTO> getSingleBudgetStatus(
			@PathVariable String budgetId,
			Authentication authentication
	) {
		String userId = tokenService.extractUserIdFromToken(authentication);
		return ResponseEntity.of(Optional.ofNullable(budgetService.getBudgetStatus(BudgetId.newOf(budgetId), userId)));
	}
}
