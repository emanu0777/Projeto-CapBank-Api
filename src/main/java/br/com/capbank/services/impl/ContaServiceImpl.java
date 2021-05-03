package br.com.capbank.services.impl;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.com.capbank.dtos.ContaDTO;
import br.com.capbank.dtos.DepositoDTO;
import br.com.capbank.entitades.Conta;
import br.com.capbank.mapper.ClienteMapper;
import br.com.capbank.mapper.ContaMapper;
import br.com.capbank.repositories.ContaRepository;
import br.com.capbank.services.ClienteService;
import br.com.capbank.services.ContaService;

@Service
public class ContaServiceImpl implements ContaService{

	@Autowired
	private ContaRepository contaRepository;
	
	@Autowired
	private ClienteService clienteService;
	
	@Autowired
	private ClienteMapper clienteMapper;
	
	@Autowired
	private ContaMapper mapper;

	@Override
	public ContaDTO abreConta(ContaDTO contaDTO) {
		if (isContaValida(contaDTO)) {
			contaDTO.setDataAbertura(new Date());
			if (contaRepository.existsByNumeroAgenciaAndNumeroConta(contaDTO.getNumeroAgencia(), contaDTO.getNumeroConta())) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Conta já cadastrada");
			}
			return mapper.toDTO(contaRepository.save(mapper.toEntity(contaDTO)));
		}
		
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados inválidos");
	}

	@Override
	public List<ContaDTO> listaContas() {
		return mapper.toDTOList(contaRepository.findAll());
	}

	@Override
	public ContaDTO buscaContaPorId(Integer idConta) {
		Optional<Conta> contaRetornada =  contaRepository.findById(idConta);
		if (!contaRetornada.isPresent()) {
			return null;
		}
		
		return mapper.toDTO(contaRetornada.get());
	}

	@Override
	public ContaDTO buscaContaPorNumeroAngenciaNumeroConta(String numeroAgencia, String numeroConta) {
		ContaDTO contaRetornada = mapper.toDTO(contaRepository.findByNumeroAgenciaAndNumeroConta(numeroAgencia,numeroConta));
		if (Objects.nonNull(contaRetornada)) {
			return contaRetornada;
		}
		
		return null;
	}

	@Override
	public ContaDTO realizaSaque(String numeroAgencia, String numeroConta, double valorSaque) {
		ContaDTO contaRetornada = buscaContaPorNumeroAngenciaNumeroConta(numeroAgencia, numeroConta);
		if (Objects.nonNull(contaRetornada)) {			
			contaRetornada = debitaValor(contaRetornada, valorSaque);
			return mapper.toDTO(contaRepository.save(mapper.toEntity(contaRetornada)));
		}
		return null;
	}

	@Override
	public DepositoDTO realizaDeposito(DepositoDTO depositoDTO) {
		
		ContaDTO contaOrigem = 
				buscaContaPorNumeroAngenciaNumeroConta(depositoDTO.getContaOrigem().getNumeroAgencia(),
				depositoDTO.getContaOrigem().getNumeroConta());
		
		ContaDTO contaDestino = 
				buscaContaPorNumeroAngenciaNumeroConta(depositoDTO.getContaDestino().getNumeroAgencia(),
				depositoDTO.getContaDestino().getNumeroConta());
		
		if (!Objects.nonNull(contaOrigem) || !Objects.nonNull(contaDestino)) {
			return null;
		}
		contaOrigem = debitaValor(contaOrigem, depositoDTO.getValorDeposito());
		contaDestino = creditaValor(contaDestino, depositoDTO.getValorDeposito());
		
		contaOrigem  =  mapper.toDTO(contaRepository.save(mapper.toEntity(contaOrigem)));
		contaDestino =  mapper.toDTO(contaRepository.save(mapper.toEntity(contaDestino)));
		
		depositoDTO.setContaOrigem(contaOrigem);
		depositoDTO.setContaDestino(contaDestino);
		
		return depositoDTO;
	}
	
	private ContaDTO debitaValor(ContaDTO contaDTO, double valorDebito) {
		contaDTO.setSaldo(contaDTO.getSaldo() - valorDebito);
		return contaDTO;
		
	}
	
	private ContaDTO creditaValor(ContaDTO contaDTO, double valorCredito) {
		contaDTO.setSaldo(contaDTO.getSaldo() + valorCredito);
		return contaDTO;
	}
	
	private Boolean isContaValida(ContaDTO contaDTO) {
		if (contaDTO.getNumeroConta() != null && contaDTO.getNumeroConta().matches("^\\d{6}") 
			&& contaDTO.getNumeroAgencia() != null &&  contaDTO.getNumeroAgencia().matches("^\\d{4}")
			&& Objects.nonNull(contaDTO.getCliente()) && contaDTO.getCliente().getCpf() != null 
			&& contaDTO.getCliente().getCpf().matches("^\\d{11}")) {
			
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
}
