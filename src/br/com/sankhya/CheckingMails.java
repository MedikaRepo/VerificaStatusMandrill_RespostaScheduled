package br.com.sankhya;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import com.microtripit.mandrillapp.lutung.MandrillApi;
import com.microtripit.mandrillapp.lutung.controller.MandrillMessagesApi;
import com.microtripit.mandrillapp.lutung.view.MandrillMessageInfo;
import com.microtripit.mandrillapp.lutung.view.MandrillSearchMessageParams;
import br.com.sankhya.VerificaResposta;

public class CheckingMails implements ScheduledAction
{

	static StringBuffer mensagem = new StringBuffer();
	static BigDecimal nunota=BigDecimal.ZERO;
	static String resposta;
	static int qtdeEnvios=0;
	static int codigoAtual=0;
	static int contExecution=0;
	static Statement smnt=null;
	static MandrillApi mandrillApi = new MandrillApi("IauY7xaaZLUaaso-vtAUFA");
	static String host = "pop3.medika.com.br";// change accordingly
	static String username = "vendas@medika.com.br";// change accordingly
	static String password = "M3dika2017";// change accordingly

	public static Object ExecutaComandoNoBanco(String sql, String op, Statement smnt)
	{
		try
		{
			

			if(op=="select")
			{
				smnt.execute(sql);
				ResultSet result = smnt.executeQuery(sql); 
				result.next();
				Object retorno = result.getObject(1);
				return retorno;

			}
			else if(op=="alter")
			{
				smnt.executeUpdate(sql);
				return (Object)1;
			}
			else
			{
				return null;
			}
		}
		catch(SQLException ex)
		{
			System.err.println("SQLException: " + ex.getMessage());
			mensagem.append("Erro ao obter campo SQL("+ex.getMessage()+") \n");
			return null;
		}
	}

	public static void check() 
	{

		try {
			Statement smnt = ConnectMSSQLServer.statement;
			String statusEntrega=""; 
			MandrillSearchMessageParams params=new MandrillSearchMessageParams();
			MandrillMessagesApi mensagens = mandrillApi.messages();

			String comando="";
			comando="SELECT DISTINCT CAST(DHENVIO AS DATE) FROM AD_EMAILMONITOR WHERE " + 
					" DHENVIO >GETDATE() -15 AND REMETENTE<>'' " +
					" GROUP BY DHENVIO "+
					" ORDER BY CAST(DHENVIO AS DATE) DESC";

			System.out.println(comando);

			smnt.execute(comando);
			ResultSet result = smnt.executeQuery(comando);
			
			try
			{
				int cont = 0;
				java.util.Date dia = new java.util.Date(); 
				for(int i=0; i<15; i++)
				{
					cont++;
					System.out.println(cont);
					dia.setDate(dia.getDate()-i);
					
					System.out.println(dia);
					params.setDateFrom(dia);
					params.setDateTo(dia);
					params.setLimit(1000);

					try
					{
						MandrillMessageInfo[] mmi= mandrillApi.messages().search(params);
						for (int j = 0; j < mmi.length; j++) 
						{

							System.out.println(mmi.length);
							System.out.println(j);
							int tagNunota=0;

							tagNunota= mmi[j].getTags().size()-1;
							resposta = "";
							System.out.println(mmi[j].getTags().get(0));
							resposta=VerificaResposta.VerificaRespostaPop3(host, username, password, mmi[j].getTags().get(tagNunota), "N");
							
							String comandoSelect;
							comandoSelect = "SELECT RESPONDIDO FROM AD_EMAILMONITOR WHERE CODEMAILMONITOR = "+mmi[j].getTags().get(0);
							String retornoSelect;
							retornoSelect = (String) ExecutaComandoNoBanco(comandoSelect, "select", smnt);
							if (retornoSelect != null){
								retornoSelect = retornoSelect.replaceAll(" ", "");
								retornoSelect = retornoSelect.replaceAll("\n", "");
							}
							if (resposta != null)  { 
								String comando1;
								comando1="update ad_emailmonitor set statusrecebimento='"+statusEntrega+
										"', statusabertura= 'Aberto "+mmi[j].getOpens()+" vezes', statusclick="+
										"'Clicado "+mmi[j].getClicks()+" vezes', respondido='"+resposta+
										"', dhultatualizacao=getdate()"
										+   " where codemailmonitor="+mmi[j].getTags().get(0);
								System.out.println(comando1);

								ExecutaComandoNoBanco(comando1, "alter", smnt);
							}else  {
								String comando1;
								comando1="update ad_emailmonitor set statusrecebimento='"+statusEntrega+
										"', statusabertura= 'Aberto "+mmi[j].getOpens()+" vezes', statusclick="+
										"'Clicado "+mmi[j].getClicks()+" vezes',"+
										" dhultatualizacao=getdate()"
										+   " where codemailmonitor="+mmi[j].getTags().get(0);
								System.out.println(comando1);

								ExecutaComandoNoBanco(comando1, "alter", smnt);
							}
							
						}
					}catch (Exception e) {
						// TODO: handle exception

						System.out.println("Erro Mandrill");

					}
				}
				System.out.println("Fim da atualização de status do MailChimp.");
				
				resposta=VerificaResposta.VerificaRespostaPop3(host, username, password, "0", "S");
				System.out.println("Mensagens Apagadas");
			}catch (Exception e) {

				System.out.println("Erro Mandrill. " + e.getMessage());
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onTime(ScheduledActionContext arg0) 
	{
		//Conecta no banco do Sankhya
		ConnectMSSQLServer.dbConnect("jdbc:sqlserver://192.168.0.5:1433;DatabaseName=SANKHYA_PROD;", "adriano","Compiles23");

		try
		{
			//Verifica os envios e outros eventos do mailChimp e atualiza a tabela de monitoramento
			check();
		}catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}


}

