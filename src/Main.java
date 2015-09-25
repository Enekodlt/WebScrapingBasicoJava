
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
	
	private static String carpetaFicheros = "/home/eneko/Documentos/";
	//El sufijo es -page-2, así que para meterlo en un bucle quitamos el número y probamos hasta que no devuelva nada
	private static String sufijoPaginacion= "-page";
	
	public static void main(String[] args) {
		
	   String ruta = "http://stackoverflow.com/questions/918237412";
	   String ficheroOferta = "Abrigos.txt";
	   int codigo = getStatusConnectionCode(ruta);
	   System.out.println("Código de la web : "+codigo);
	   
	   if(codigo==200){
		   crearOComprobarArchivo(ruta, carpetaFicheros+ficheroOferta);
	   }else{
		   JOptionPane.showMessageDialog(new JFrame(), "No se ha podido cargar la página.\n El status code que "
		   		+ "devuelve la página es: "+codigo);
	   }

		System.exit(0);
	}
	
	public static void crearOComprobarArchivo(String url, String nombreArchivo){
			File archivo = new File(nombreArchivo);
		    
	        if (!archivo.exists()){
	        	String codHtml = leerPagina(url);
	        	if (escribirArchivo(archivo, codHtml)==true){
	        		String ar[]=nombreArchivo.split("/");
	        		String nombreLimpio = ar[(ar.length-1)];
	    			JOptionPane.showMessageDialog(new JFrame(), "Se ha creado el archivo correctamente: "+nombreLimpio 
	    					+"\nEn la carpeta:\n"+carpetaFicheros);
	    		}
	        }else{
	        	String codHtml = leerPagina(url);
	        	ArrayList<String[]> diferenciaArtículos = compararArchivoYCodigo(archivo, codHtml);
	        	if (!diferenciaArtículos.isEmpty()){
	        		int cantidadNuevos=diferenciaArtículos.size();
	        		String nuevosTexto="";
	        		for(int i=0;i<cantidadNuevos;i++){
	        			nuevosTexto+=diferenciaArtículos.get(i)[0]+"\n";
	        		}
	        		String[] botones = {"Visitar página", "En otro momento"};
	        		int respuesta = JOptionPane.showOptionDialog(null, "Hay "+cantidadNuevos+" novedades en abrigos:\n"
	        		+nuevosTexto,
	        				"Nuevos abrigos", JOptionPane.INFORMATION_MESSAGE, 1, null, botones, botones[0]);
	        		if(respuesta == 0){
	        			//Se pueden abrir pestañas de firefox de 2 en 2. Hay que añadir un espacio entre dos URL
	            		try {
	            			for(int x=0;x<diferenciaArtículos.size();x++){
	            				String dirAbrigo=diferenciaArtículos.get(x)[1].trim();
	            				Process p = Runtime.getRuntime().exec("firefox -new-tab http://www.esprit.es/"+dirAbrigo);
	            				Thread.sleep(1000);
	            			}
	    				} catch (IOException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    				}catch (InterruptedException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    				}
	        		} else {

	        		}
	        	}else{
	    			JOptionPane.showMessageDialog(new JFrame(), "No hay nuevos abrigos");
	    		}
	        }
	}
	
	public static String leerPagina(String stringUrl){
		InputStream is = null;
	    BufferedReader lectorHtml;
	    String linea, documentoHtmlCompleto, documentoHtmlCompletoAnterior, documentoAGuardar;
        documentoHtmlCompletoAnterior="";
        documentoAGuardar = "";
        int x=1;
	    try {
	    	URL url;
	    	do{
	    		documentoHtmlCompleto = "";
	    		if(x>1){
	    			url = new URL(stringUrl+sufijoPaginacion+x);
	    		}else{
	    			url = new URL(stringUrl);
	    		}
	    		System.out.println(url);
		        is = url.openStream();  // throws an IOException
		        lectorHtml = new BufferedReader(new InputStreamReader(is));
		        while ((linea = lectorHtml.readLine()) != null) {
		            documentoHtmlCompleto += "\n"+linea;
		        }
		        ArrayList<String> diferencia= compararHtml(documentoHtmlCompleto, documentoHtmlCompletoAnterior);
		        if(!diferencia.isEmpty()){
		        	String nuevosTexto="";
	        		for(int i=0;i<diferencia.size();i++){
	        			nuevosTexto+=diferencia.get(i)+"\n";
	        		}
	        		//System.out.println(nuevosTexto);
		        	documentoHtmlCompletoAnterior = documentoHtmlCompleto;
			        Document doc = Jsoup.parse(documentoHtmlCompleto);
			        //Extraigo en otro documento solo el div con la lista
			        Document doc2 = Jsoup.parse(doc.getElementById("styleoverview").html());
			        
			        Elements articulos = doc2.getElementsByClass("style");
			        for (Element articulo : articulos){
			        	documentoAGuardar += "\n"+articulo.select("p.style-name span:nth-child(2)").text() + " -- "+articulo.getElementsByTag("a").attr("href");
			        }
			        System.out.println("pagina "+x);
			        x++;
		        }else{
		        	break;
		        }
	    	}while(x>1);
		} catch (MalformedURLException mue) {
         mue.printStackTrace();
		} catch (IOException ioe) {
         ioe.printStackTrace();
		} finally {
	        try {
	            if (is != null) is.close();
	        } catch (IOException ioe) {
	            // nothing to see here
	        }
		} 
	    return documentoAGuardar;
	}
	
	public static Boolean escribirArchivo(File archivo, String codHtml){
		FileWriter escritor = null;
		Boolean escribeBien;
		try{
			escritor = new FileWriter(archivo);
			escritor.write(codHtml);
			
			escribeBien = true;
		}catch(FileNotFoundException e){
			System.out.println("No existe el fichero o la carpeta");
			JOptionPane.showMessageDialog(new JFrame(), "Ha ocurrido algún error creando el nuevo fichero"
					+" \n\nEn la carpeta:\n"+carpetaFicheros);
			escribeBien = false;
		}catch(IOException e){
			System.out.println("algo ha fallado");
			JOptionPane.showMessageDialog(new JFrame(), "Ha ocurrido algún error creando el nuevo fichero"
					+" \n\nEn la carpeta:\n"+carpetaFicheros);
			escribeBien = false;
		}finally{
			if (escritor != null){
				try {
					escritor.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return escribeBien;
	}
	
	public static ArrayList<String[]> compararArchivoYCodigo(File fil, String codHtml){
		FileReader archivo;
		BufferedReader lector = null;
		ArrayList<String[]> diferencia = new ArrayList<String []>();		
		try{
			archivo = new FileReader(fil);
			lector = new BufferedReader(archivo);
			String linea, codArchivo;
			codArchivo = "";
			while ((linea = lector.readLine()) != null){
				codArchivo += "\n"+linea;
			}

			BufferedReader reader = new BufferedReader(new StringReader(codHtml));
			BufferedReader reader2;
			
			String linea1, linea2;
			Boolean repetida;
			while ((linea1 = reader.readLine()) != null){
				repetida = false;
				reader2 = new BufferedReader(new StringReader(codArchivo));
				while((linea2 = reader2.readLine()) != null){
					if(linea1.equals(linea2)){
						repetida = true;
						break;
					}
				}
				if(!repetida){
					String[] datos= linea1.split("--");
					diferencia.add(datos);
				}
			}
		}catch(FileNotFoundException e){
			
		}catch (IOException e) {
			
		}finally{
			if (lector != null){
				try {
					lector.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return diferencia;
	}
	
	public static ArrayList<String> compararHtml(String codHtml, String codHtml2){
		BufferedReader lector = null;
		ArrayList<String> diferencia = new ArrayList<String >();		
		try{

			BufferedReader reader = new BufferedReader(new StringReader(codHtml));
			BufferedReader reader2;
			
			String linea1, linea2;
			Boolean repetida;
			while ((linea1 = reader.readLine()) != null){
				repetida = false;
				reader2 = new BufferedReader(new StringReader(codHtml2));
				while((linea2 = reader2.readLine()) != null){
					if(linea1.equals(linea2) || linea1.contains("data-pagenumber") || linea1.contains("applicationTime")){
						repetida = true;
						break;
					}
				}
				if(!repetida){
					diferencia.add(linea1);
				}
			}
		}catch(FileNotFoundException e){
			
		}catch (IOException e) {
			
		}finally{
			if (lector != null){
				try {
					lector.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return diferencia;
	}
	
	/**
	 * Con esta método compruebo el Status code de la respuesta que recibo al hacer la petición
	 * EJM:
	 * 		200 OK			300 Multiple Choices
	 * 		301 Moved Permanently	305 Use Proxy
	 * 		400 Bad Request		403 Forbidden
	 * 		404 Not Found		500 Internal Server Error
	 * 		502 Bad Gateway		503 Service Unavailable
	 * @param url
	 * @return Status Code
	 */
	public static int getStatusConnectionCode(String url) {
			
	    Response response = null;
		
	    try {
		response = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(100000).ignoreHttpErrors(true).execute();
	    } catch (IOException ex) {
		System.out.println("Excepción al obtener el Status Code: " + ex.getMessage());
	    }
	    return response.statusCode();
	}
}