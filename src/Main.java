
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
	
	//Yo le indico la carpeta en la que quiero que lo guarde.
	//Cambialo o quítalo si quieres y el archivo aparecerá en workspace.
	private static String carpetaFicheros;
	//El sufijo es -page2, así que dejamos el String listo para añadirle un parámetro con el número de página
	private static String sufijoPaginacion= "-page%s";
	private static long inicio;

	public static void main(String[] args) {
		carpetaFicheros = System.getProperty("user.dir")+"/";
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
	        	String textoProcesado = leerPagina(url);
	        	if (escribirArchivo(textoProcesado, archivo)){
	        		String ar[]=nombreArchivo.split("/");
	        		String nombreLimpio = ar[(ar.length-1)];
	        		long fin = System.currentTimeMillis() - inicio;
	    			JOptionPane.showMessageDialog(new JFrame(), "Tiempo transcurrido: "+fin+"\nSe ha creado el archivo correctamente: "+nombreLimpio 
	    					+"\nEn la carpeta:\n"+carpetaFicheros);
	    		}
	        }else{
	        	String textoProcesado = leerPagina(url);
	        	ArrayList<String[]> diferenciaArtículos = compararArchivoYCodigo(archivo, textoProcesado);
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
	        			/*Se pueden abrir pestañas de firefox de 2 en 2. Hay que añadir un espacio entre dos URL
	        			Si se abren más de 2 a la vez, abre una nueva ventana con todas las pestañas.
	        			Yo prefiero que se vayan abriendo poco a poco en la misma ventana que estoy*/	            		
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
	
	/**
	 * Lee la página que pasemos como parámetro, filtra el texto y devuelve el resultado
	 * @param stringUrl
	 * @return
	 */
	public static String leerPagina(String stringUrl){
	    String  documentoProcesado, documentoProcesadoAnterior, documentoAGuardar;
	    documentoProcesadoAnterior="";
        documentoAGuardar = "";
        int x=1;
    	String url;
    	Boolean iguales = false;
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
	
	/**
	 * Escribe un texto dentro del archivo
	 * @param archivo
	 * @param textoProcesado
	 * @return
	 */
	public static Boolean escribirArchivo(String textoProcesado, File archivo){
		FileWriter escritor = null;
		Boolean escribeBien;
		try{
			escritor = new FileWriter(archivo);
			escritor.write(textoProcesado);
			escribeBien = true;
		}catch(FileNotFoundException e){
			System.out.println("No existe el fichero o la carpeta");
			JOptionPane.showMessageDialog(new JFrame(), "Ha ocurrido algún error creando el nuevo fichero"
					+" \n\nEn la carpeta:\n"+carpetaFicheros);
			escribeBien = false;
		}catch(IOException e){
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
	
	/**
	 * Compara el texto del archivo donde guardamos con otra cadena
	 * @param fil
	 * @param textoProcesado
	 * @return
	 */
	public static ArrayList<String[]> compararArchivoYCodigo(File fil, String textoProcesado){
		FileReader archivo;
		BufferedReader lector = null;
		ArrayList<String[]> diferencia = new ArrayList<String []>();		
		try{
			BufferedReader reader = new BufferedReader(new StringReader(textoProcesado));
			archivo = new FileReader(fil);
			lector = new BufferedReader(archivo);
			String linea1, linea2;
			while ((linea1 = lector.readLine()) != null){
				linea2=reader.readLine();
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