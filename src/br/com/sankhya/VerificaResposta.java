package br.com.sankhya;

import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import jdk.nashorn.internal.runtime.Debug;

public class VerificaResposta {


	public static String VerificaRespostaPop3(String host, String user, String password, String nunota) throws MessagingException
	{
		String retorno="";
		//Verifica se teve resposta do cliente

		//create properties field
		Properties properties = new Properties();
        		
		properties.put("mail.pop3.host", host);
		properties.put("mail.pop3.port", "995");
		properties.put("mail.pop3.starttls.enable", "true");
		Session emailSession = Session.getInstance(properties);

		//create the POP3 store object and connect with the pop server
		Store store;
		Folder emailFolder = null;
		try {
			store = emailSession.getStore("pop3s");

			try
			{
				store.connect(host, user, password);

				//create the folder object and open it
				emailFolder = store.getFolder("INBOX");
				emailFolder.open(Folder.READ_WRITE);

				// retrieve the messages from the folder in an array and print it
				Message[] messages = emailFolder.getMessages();


				//APAGA MENSAGENS QUE NÃO SÃO REPOSTAS
				String Assunto2="";
				for (int i = messages.length-1; i > 0; i--) 
				{

					Message message = messages[i];
					if(message.getSubject()!=null)
					{
						String assunto=message.getSubject();
						Assunto2=assunto;
						if (assunto.contains("Re: Proposta Comercial")==false)
						{
							message.setFlag(Flags.Flag.DELETED, true);
						//	System.out.println(assunto);
						}
					}

				}

				for (int i = messages.length-1; i > 0; i--) 
				{
					Message message = messages[i];
					String assunto=message.getSubject();

						if(assunto.contains("Re: Proposta Comercial - "+nunota))
						{
							retorno= "Respondido: "+ assunto+" - "+message.getSentDate().toString()+"\n"; 
						}
				}
				if(retorno=="")
				{
					//Se não encontrar o assunto do email completo com de resposta, busca só número da propsta
					for (int i = messages.length-1; i > 0; i--) 
					{
						Message message = messages[i];
						String assunto=message.getSubject();
						if(assunto.contains(nunota))
						{
							retorno= "Respondido: "+ assunto+" - "+message.getSentDate().toString()+"\n"; 
						}
						else
						{
							retorno="N�o respondido. \n";
						}

					}
				}

				//APAGA MENSAGENS COM MAIS DE 60 DIAS
				//				for (int i = messages.length-1; i > 0; i--) 
				//				{
				//					Message message = messages[i];
				//
				//					try
				//					{
				//						Date data=new Date();
				//						GregorianCalendar dataCal = new GregorianCalendar();
				//						dataCal.setTime(data);
				//						int mes = dataCal.get(Calendar.MONTH);
				//				        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
				//				        data.setMonth(mes-2);
				//				        @SuppressWarnings("deprecation")
				//				        Date dateA = df.parse(df.format(data));
				//				        Date dateB = df.parse(df.format(message.getReceivedDate()));
				//
				//						if (dateB.before(dateA))
				//						{
				//							message.setFlag(Flags.Flag.DELETED, true);		
				//						}
				//					}catch(Exception e)
				//					{
				//						retorno=retorno+e.getMessage();	
				//					}
				//				}

				//close the store and folder objects
				emailFolder.close(true);
				store.close();
			}
			catch (Exception e)
			{
				CheckingMails.mensagem.append("Erro ao verificar resposta. "+e.getMessage());
				//close the store and folder objects
				emailFolder.close(true);
				store.close();
			}
		} catch (NoSuchProviderException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			CheckingMails.mensagem.append("Erro ao verificar resposta. "+e1.getMessage());
		}
		return retorno;
	}
}
