# Servidor de impresion, como servicio. para windows.

Para este proyecto se esta usando apache common daemon para crear el servicio
Se estan usando las siguientes tecnologias.
1- Gson
2- EscPos de anastaciocintra
3- Apache common daemon
4- Procrun - prunsrv.exe para crear el servicio en windows
5- InnoSetup
6- Launch4j

### parametros usados
> Creacion del servicio

```cmd
./prunsrv.exe //IS//PrintServer --DisplayName="Servidor de impresion" --Description="Servidor de impresion de cdsfot" --Startup=auto --StartMode=exe --StartImage=E:/cesar/Documents/servidor_impresion/PrintServer.exe --StopMode=exe --StopImage=E:/cesar/Documents/servidor_impresion/PrintServer.exe --StopParams=stop --ServiceUser=LocalSystem --StartPath=E:/cesar/Documents/servidor_impresion/
```

### Explicación de los parámetros usados:

  -  //IS//PrintServer: Registra el servicio con el nombre "PrintServer".

  -  --Description: Descipcion del servicio.

  -  --DisplayName: Nombre del servicio
  -  --Startup: forma de arranque automatico 
  -  --StartMode: forma de iniciar el servicio en este caso exe, por que tengo un exe, si fuera un jar seria jvm 
  -  --StartImage: Imagen que ejecutara, se le pasa la ruta del exe 
  -  --StopMode: forma de parar el servicio, en este caso exe
  -  --StopImage: direccion de el exe  
  -  --StopParams: parametros de parar el servicio en este caso stop, que esta en mi clase main
  -  --ServiceUser: permiso, es este caso como administrador que es LocalSystem 
  -  --StartPath: darle permiso a la carpeta donde nesecita trabajar. 

### Una vez creado el servicio se gestionan de la siguiente forma

- Iniciar servicio

```cmd
net start PrintServer
```

- Detener servicio

```cmd
net stop PrintServer
```

- Eliminar servicio

```cmd
./prunsrv.exe //DS//PrintServer
```

### jlink
se uso jlink que es una herramiento que viene en el jdk, que sirve para crear un JVM custom con lo necesario que nesecita nuestra app o jar para funcionar.
para saber que dependencias nesecita mi app corro el siguiente comando
```cmd
jdeps --print-module-deps --ignore-missing-deps -recursive PrintServer-1.0-SNAPSHOT-jar-with-dependencies.jar
```
esto devuelve por ejemplo en mi caso
```cmd
java.base
```
entonces solo eso nesecito.
- Para crear el runtime custom corro el siguiente comando 
```cmd
jlink --module-path "C:\Program Files\Amazon Corretto\jdk23.0.2_7\jmods" --add-modules java.base --output runtime
```
esto creara un carpeta runtime en el directorio actual com mi jvm custom, para el funcionamiento correcto de mi app.
> Ojo esto lo realizo para que esto sea liviano y balla en mi instalador, pero bien puedes instalar el JRE por aparte sin ningun problema y no haces este procedimeitno.

#### JRE-JDK de OpenJdk
- Amazon Corretto
- Zulu
- AdoptOpenJDK

### Solo JRE para instalacion en cliente, para que sea mas liviano
- Bellsoft - [https://bell-sw.com/pages/downloads/?version=java-23&os=windows](https://bell-sw.com/pages/downloads/)

### configuracion de firewall
esto para que el firewall permita entradas a este puerto del servidor, en caso de ser necesario
se Incluyo en el script de instalacion.
```cmd
netsh advfirewall firewall add rule name="Allow Port 8088" dir=in action=allow protocol=TCP localport=8088
```
