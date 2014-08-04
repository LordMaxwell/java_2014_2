package br.com.stefanini.treinamento.boleto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import br.com.stefanini.treinamento.exception.ManagerException;

public abstract class BloquetoBBImpl implements BloquetoBB {

	protected String codigoBanco;
	protected String codigoMoeda;
	protected String fatorVencimento;
	protected Date dataVencimento;
	protected Date dataBase;
	protected BigDecimal valor;
	protected String numeroConvenioBanco;
	protected String complementoNumeroConvenioBancoSemDV;
	protected String numeroAgenciaRelacionamento;
	protected String contaCorrenteRelacionamentoSemDV;
	protected String tipoCarteira;

	private int dvCodigoBarras;

	protected abstract void validaDados() throws ManagerException;

	/**
	 * Inicializa o fator de vencimento
	 */
	protected void setFatorVencimento() {

		long dias = diferencaEmDias(dataBase, dataVencimento);

		/*
		 * Este m�todo recebe o valor da diferen�a em dias
		 * da data base com a data do vencimento e armazena 
		 * na vari�vel fator vencimento no formato adequado.
		 */

		fatorVencimento = String.format("%04d", dias);

	}

	/**
	 * Inicializa os valores, formata
	 */
	protected void init() {

		setFatorVencimento();

	}

	/**
	 * Retorna o valor formatado do boleto banc�rio
	 * 
	 * @return
	 */
	protected String getValorFormatado() {
		/*
		 * O m�todo retorna o valor armazenado em "value" no formato adequado,
		 * sendo este formato apresentando apenas algarismos, sem ponto, 
		 * com 10 casas obrigatoriamente, completadas com zeros n�o significativos
		 * caso necess�rio, e com duas casas depois da v�rgula, 
		 * sendo arredondado para cima caso hajam mais casas. 
		 */

		return String.format(
				"%010d",
				Long.valueOf(valor.setScale(2, RoundingMode.HALF_UP).toString()
						.replace(".", "")));
	}

	/**
	 * Formata o n�mero do conv�nio da Linha Digit�vel
	 * 
	 * @return
	 */
	protected abstract String getLDNumeroConvenio();

	/**
	 * Retorna o c�digo de barras do Bloqueto
	 * 
	 * @return c�digo de barras
	 */
	protected abstract String getCodigoBarrasSemDigito();

	public abstract String getCodigoBarras();

	/**
	 * Campo 5 da Linha Digit�vel
	 * 
	 * @return
	 */
	private String ldCampo5() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(fatorVencimento);
		buffer.append(getValorFormatado());
		return buffer.toString();
	}

	/**
	 * Campo 4 da Linha Digit�vel
	 * 
	 * @return
	 */
	private String ldCampo4() {
		return String.valueOf(digitoVerificadorCodigoBarras(getCodigoBarrasSemDigito()));
	}

	/**
	 * Campo 3 da Linha Digit�vel
	 * 
	 * @return
	 */
	private String ldCampo3() {
		return String.format("%s.%s", getCodigoBarras().substring(34, 39), getCodigoBarras().substring(39, 44));
	}

	/**
	 * Campo 2 da Linha Digit�vel
	 * 
	 * @return
	 */
	private String ldCampo2() {
		return String.format("%s.%s", getCodigoBarras().substring(24, 29), getCodigoBarras().substring(29, 34));
	}

	/**
	 * Calcula o digito verificador do campo
	 * 
	 * @param campo
	 * @return
	 */
	protected int digitoVerificadorPorCampo(String campo) {
		int soma = 0;
		int parcial = 0;
		boolean alt = true;
		for (int i = campo.length()-1; i >= 0; i--) {
			if(alt){
				parcial = Integer.parseInt(campo.substring(i)) * 2;
				while(parcial >= 10){
						parcial = (parcial % 10)+((int)(parcial / 10));
				}
			}else{
				parcial = Integer.parseInt(campo.substring(i-1 , i));
			}
			alt = !alt;
			soma += parcial;
		}
		if((10 - (soma % 10)) == 10){
			return 0;
		}else{
			return 10 - (soma % 10);
		}
	}

	/**
	 * Calcula o digito verificado do c�digo de barras
	 * 
	 * @param codigoBarras
	 * @return
	 */
	protected int digitoVerificadorCodigoBarras(String codigoBarras) {
		int i = codigoBarras.length();
		int k = 2;
		int soma = 0;
		
		while(i != 0){
		
			if (k == 10){
				k = 2;
			}
			soma += Integer.parseInt(codigoBarras.substring(i-1 , i)) * k;
			k++;
			i--;
		}
		soma = soma%11;
		soma = 11 - soma;

		if ((soma <= 0) || (soma == 10) || (soma == 11)){
			return 1;
		}else{
			return soma;
		}
	}

	/**
	 * Campo 1 da Linha Digit�vel
	 * 
	 * - C�digo do Banco - C�digo da Moeda - N�mero do conv�nio
	 * 
	 * @return
	 */
	private String ldCampo1() {
		StringBuilder buffer = new StringBuilder();
		
		buffer.append(codigoBanco);
		buffer.append(codigoMoeda);
		buffer.append(getLDNumeroConvenio());
		
		return buffer.toString();

	}

	public String getLinhaDigitavel() {

		init();

		StringBuilder buffer = new StringBuilder();
		buffer.append(ldCampo1());
		buffer.append(digitoVerificadorPorCampo(ldCampo1()));
		buffer.append(" ");
		
		buffer.append(ldCampo2());
		buffer.append(digitoVerificadorPorCampo(ldCampo2()));
		buffer.append(" ");
		buffer.append(ldCampo3());
		buffer.append(digitoVerificadorPorCampo(ldCampo3()));
		buffer.append(" ");
		buffer.append(ldCampo4());
		buffer.append(" ");
		buffer.append(ldCampo5());
		
		return buffer.toString();
	}

	/**
	 * Retorna a diferen�a em dias de duas datas
	 * 
	 * @param dataInicial
	 *            Data inicial
	 * @param dataFinal
	 *            Data final
	 * @return
	 */
	protected static long diferencaEmDias(Date dataInicial, Date dataFinal) {
		/*
		 * Retorna um long arredondado para cima da diferen�a 
		 * entre as duas datas do par�metro, a final menos a inicial, 
		 * e divide pela quantidade de milissegundos de um dia, 
		 * retornando assim, a quantidade da diferen�a em dias 
		 * entre as duas datas.
		 */		
		return Math
				.round((dataFinal.getTime() - dataInicial.getTime()) / 86400000D);
	}

	public int getDvCodigoBarras() {

		getCodigoBarras();

		return dvCodigoBarras;
	}
}
