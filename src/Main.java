
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
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
	
	//Yo le indico la carpeta en la que quiero que lo guarde.
	//Quítalo si quieres y el archivo aparecerá en la carpeta del proyecto.
	private static String carpetaFicheros = "/home/eneko/Documentos/";
	
	//El sufijo es -page2, así que dejamos el String listo para añadirle un parámetro con el número de página
	private static String sufijoPaginacion= "-page%s";
	private static long inicio;

	public static void main(String[] args) {
		inicio = System.currentTimeMillis();
	   String ruta = "http://www.esprit.es/chaquetas-abrigos-hombre";
	   String ficheroOferta = "Abrigos.txt";
	   
	   //Comprobar el código que devuelve la petición a la URL que le pasamos
	   int codigo = getStatusConnectionCode(ruta);
	   if(codigo==200){
		   crearOComprobarArchivo(ruta, carpetaFicheros+ficheroOferta);
	   }else{
		   JOptionPane.showMessageDialog(new JFrame(), "No se ha podido cargar la página.\n El status code que "
		   		+ "devuelve la página es: "+codigo);
	   }
		System.exit(0);
	}
	/**
	 * Compara el archivo donde guardamos la información con lo que hay actualmente en la página.
	 * Si hay diferencias da la opción de abrir las novedades en pestañas de firefox
	 * Si no existe archivo lo crea.
	 * @param url
	 * @param nombreArchivo
	 */
	public static void crearOComprobarArchivo(String url, String nombreArchivo){
			File archivo = new File(nombreArchivo);
		    
	        if (!archivo.exists()){
	        	String codHtml = leerPagina(url);
	        	if (escribirArchivo(archivo, codHtml)){
	        		String ar[]=nombreArchivo.split("/");
	        		String nombreLimpio = ar[(ar.length-1)];
	        		long fin = System.currentTimeMillis() - inicio;
	    			JOptionPane.showMessageDialog(new JFrame(), "Tiempo transcurrido: "+fin+"\nSe ha creado el archivo correctamente: "+nombreLimpio 
	    					+"\nEn la carpeta:\n"+carpetaFicheros);
	    		}
	        }else{
	        	String codHtml = leerPagina(url);
	        	ArrayList<String[]> diferenciaArtículos = compararArchivoYCodigo(archivo, codHtml);
	        	if (!diferenciaArtículos.isEmpty()){
	        		int cantidadNuevos=diferenciaArtículos.size();
	        		String nuevosTexto="";
	        		//Rellenar string nuevosTexto para mostrarlo en JOptionPane
	        		for(int i=0;i<cantidadNuevos;i++){
	        			nuevosTexto+=diferenciaArtículos.get(i)[0]+"\n";
	        		}
	        		String[] botones = {"Visitar página", "En otro momento"};
	        		int respuesta = JOptionPane.showOptionDialog(null, "Hay "+cantidadNuevos+" novedades en abrigos:\n"
	        		+nuevosTexto,
	        				"Nuevos abrigos", JOptionPane.INFORMATION_MESSAGE, 1, null, botones, botones[0]);
	        		if(respuesta == 0){
	        			//Se pueden abrir pestañas de firefox de 2 en 2. Hay que añadir un espacio entre dos URL
	        			//Si abro más de 2 a la vez, abre una nueva ventana con todas las pestañas
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
	        		long fin = System.currentTimeMillis() - inicio;
	    			JOptionPane.showMessageDialog(new JFrame(), fin+" No hay nuevos abrigos");
	    		}
	        }
	}
	
	public static String leerPagina(String stringUrl){
	    String  documentoProcesado, documentoProcesadoAnterior, documentoAGuardar;
	    documentoProcesadoAnterior="";
        documentoAGuardar = "";
        int x=1;
    	String url;
    	Boolean iguales =false;
    	/*
    	 * Esta web, devuelve contenido repetido si intentamos leer un indice de página que no existe.
    	 * Si solo hay 2 páginas e intentamos leer la 3, nos volverá a enseñar la 2.
    	 * Leemos hasta que se repita el contenido
    	 */
    	do{
    		documentoProcesado = "";
    		if(x>1){
    			url = String.format(stringUrl+sufijoPaginacion,x);
    		}else{
    			url = stringUrl;
    		}	    		
	        Document doc = getHtmlDocument(url);
	        //Extraer elemento con el div contiene todos los artículos
	        Element primerDiv = doc.getElementById("styleoverview");
	        //Extraer todos los div que contienen los datos sobre cada artículo 
	        Elements articulos = primerDiv.getElementsByClass("style");
	        for (Element articulo : articulos){
	        	//El selector span:nth-child(x) busca al padre de span y elige al elemento hijo en la posición x
	        	documentoProcesado += "\n"+articulo.select("p.style-name a span:nth-child(2)").text() + " -- "+articulo.getElementsByTag("a").attr("href");
	        }
		    if(!documentoProcesado.equals(documentoProcesadoAnterior)){
		    	documentoProcesadoAnterior = documentoProcesado;
		    	documentoAGuardar += documentoProcesado;
		        x++;
	        }else{
	        	iguales=true;
	        }
    	}while(!iguales);
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
			BufferedReader reader = new BufferedReader(new StringReader(codHtml));
			archivo = new FileReader(fil);
			lector = new BufferedReader(archivo);
			String linea1, linea2;
			Boolean repetida;
			while ((linea1 = lector.readLine()) != null){
				repetida = false;
				linea2=reader.readLine();
				System.out.println(linea1);
				System.out.println(linea2);
				if(!linea1.equals(linea2)){
					String[] datos= linea1.split("--");
					diferencia.add(datos);
				}	
			}		
		}catch(FileNotFoundException e){
			e.getMessage();
		}catch (IOException e) {
			e.getMessage();
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
	
	/**
	 * Con este método devuelvo un objeto de la clase Document con el contenido del
	 * HTML de la web que me permitirá parsearlo con los métodos de la librelia JSoup
	 * @param url
	 * @return Documento con el HTML
	 */
	public static Document getHtmlDocument(String url) {

	    Document doc = null;
		try {
		    doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(100000).get();
		} catch (IOException ex) {
			System.out.println("Excepción al obtener el HTML de la página" + ex.getMessage());
		}
	    return doc;
	}
}