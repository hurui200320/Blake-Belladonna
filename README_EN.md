# Blake-Belladonna

An open source burn-after-reading service based on Java and Javalin.

**中文版在[这里](./README.md).**

[TOC]

## Introduction

This is a website offer to send burn-after-reading messages, which is based on Java and Javalin framework. You can share throwaway message with it. You first store your message on the server, then server give you a link. Share it and everything is done! Once someone access the link and server get the request, your message will be deleted immediately after rendering the website that showing your message.  Then your message is gone, disappeared forever.

You can [click here to create a burn-after-reading message](https://blake.skyblond.info/create).

## Detailed instructions and brief principles

### Create message

You can write anything you want when creating a message. But there is an attention on the website writing in red, reading: ***Please be aware that the message you're editing is going to be stored on this server, WITHOUT ANY ENCRYPTION. Which means the hoster of the server may SEE WHAT YOU WRITE BELOW. So please DO NOT WRITE ANYTHING PRIVATE BELOW, unless it has been encrypted properly, and YOU SHOULD BEAR THE CONSEQUENCES OF PRIVACY LEAK.*** If you want an encryption, I recommend something safe, such as AES. And ***DO NOT*** share your ciphertext and key ***both*** through this website. Please consider anther secure channel, such as SMS and phone call.

### Share message

After submitting message successfully, there will be a link shown on the website. Copy and share it. Please note: there is no preview function of the link. The moment you access it, the moment it invalid. You can submit multiple times to see the appearance of your message. ***But please be aware that the message will be deleted automatically in 7 days. And once you lost your link, you can't find it back.***

### Query message

Access the link via web browser, the message will be rendered as a page. There will be notes in red saying this is a burn-after-reading message and you will never read it again after you close or refresh this page. If there are no message found or the message has been deleted, server will respond with status code `410`.

### Advance

You can use html language to enrich your message's appearance.

Everything you input will be translated into HTML paragraph, which surrounding with `<p>` and `</p>`. You can put `#!&$>` on the start of your line and that line won't be translated. 

***However: your html codes may cause the web page showing improperly, which would lead your message shown in an abnormal way.***

## Detailed explanation of the principle for developers

### Web interface

[Javalin](https://javalin.io/) handled all web interface. The program registered 4 paths. It doesn't care about the web interactions, but only process the requests. The theme showing message and create are modified from my email template. You can find more about it [here](https://www.skyblond.info/archives/2018/10/27/529)(Chinese only).

#### `/create` `GET`

This path handles creating messages. All it does are only two lines:

```java
ctx.header("content-type","text/html; charset=UTF-8");
ctx.result(String.join("\n", Files.readAllLines(PropertiesUtils.getProperties().getCreateTheme())));
```

In default, it use `create.html` located in the same folder with program. When you change the template, please be careful with from actions, which need meet the needs of `new-message`, the path actually creating a message on the server. It requires `POST` action and another 2 keys, which has already been defined in `Main.class` as `POST_TITLE_FIELD` and `POST_CONTENT_FIELD`. For now they are valued `title` and `content`.

#### `/show/:checksum` `GET`

This path take the responsibility of showing a message. `:checksum` will be replaced with actual message id when being accessed to build up a real URL. The program will read `:checksum` and find the corresponding message. The code is below:

```java
Messages messages;
if(true)
//            if(PropertiesUtils.getProperties().getDataMode() == 0)
    messages = Messages.findMessageFile(ctx.pathParam("checksum").toUpperCase());
else
    messages = Messages.findMessageMysql(ctx.pathParam("checksum").toUpperCase());
if(messages == null){
    ctx.status(410);
    return;
}
ctx.header("content-type","text/html; charset=UTF-8");
ctx.result(String.join("\n", Files.readAllLines(PropertiesUtils.getProperties().getMessageTheme())).replace("{{% title %}}", messages.getTitle()).replace("{{% content %}}", messages.getContent()));
        
```

There is a `if(true)`, because I want the program store messages both in file and MySQL. But then I found that it's not necessary to implement a MySQL version, that's I leave them here. I will finish it later. Maybe.

The program will find the message first. When find nothing, the server will return the status code `410`. After finding the message, the program will render the page according to the template, in default it's `message.html` located in the same folder with program. The placeholder `{{% title %}}` will be replaced by the message's title, and `{{% content %}}` is the content of message. ***The content will be put in template directly, which means the content will be treated as html code.***

#### `/new-message` `POST`

This path creates messages by `POST` requests, which is always started by the form in `/create`. The program will check the amount of messages which are already exists.

```java
File[] list = PropertiesUtils.getProperties().getDataDirectory().toFile().listFiles();
if(list != null && list.length >= PropertiesUtils.getProperties().getMaxMessages()){
    ctx.status(500);
    ctx.result("Too many message files in data directory!");
    Share.logger.error("Too many message files in data directory!");
    return;
}
```

If the total amount reach the max number, it will return status code `500` with notice message. Otherwise the program check the parameters of the request: 

```java
Map<String, List<String>> raw = ctx.formParamMap();
if(raw.keySet().containsAll(Arrays.asList(POST_TITLE_FIELD, POST_CONTENT_FIELD))){
	...
}else {
	ctx.status(400);
	ctx.result("bad request");
}
```

The name of two parameters are defined by two constants:

```java
public static final String POST_TITLE_FIELD = "title";
public static final String POST_CONTENT_FIELD = "content";
```

If the request doesn't meet the needs, server will return status code `400` and notice message. Otherwise the program will process the message to store it:

```java
Messages messages = new Messages();
messages.setTitle(String.join("",raw.get(POST_TITLE_FIELD)));
String[] body = String.join("",raw.get(POST_CONTENT_FIELD)).split("\n");
StringBuilder sb = new StringBuilder();
boolean isRaw = false;
for(String s : body){
    if(!isRaw && s.startsWith("#!&$>") && s.length() > 5){
        sb.append(s.substring(5));
    }else if(isRaw || s.startsWith("#!&$<")){
        isRaw = true;
        if(s.startsWith("#!&$<"))
            sb.append(s.substring(5) + "\n");
        else if(s.contains("&!>")){
            sb.append(s.split("&!>")[0] + "\n");
            isRaw = false;
        }else
            sb.append(s + "\n");
    }else {
        if(s.equals(null) || s.equals(""))
            sb.append("<br>");
        else{
            s = s.replace("&", "&amp;");
            s = s.replace(" ","&nbsp;"); // html Escape characters
            s = s.replace("<", "&lt;");
            s = s.replace(">", "&gt;");
            s = s.replace("\"", "&quot;");
            sb.append("<p>" + s + "</p>\n");
        }
    }
}
messages.setContent(sb.toString());

String result;
if(true){
//if(PropertiesUtils.getProperties().getDataMode() == 0){
	result = messages.storeToFile();
}else{
	result = messages.storeToMysql();
}

if(result != null)
	ctx.result(result);
else
	ctx.status(500);
ctx.status(201);
ctx.header("content-type","text/html; charset=UTF-8");
ctx.result(String.join("\n", Files.readAllLines(PropertiesUtils.getProperties().getSucceedTheme())).replace("{{% code %}}", result));
```

The title won't change. Every line will be translated into HTML-style paragraph and replace html characters(like `& < > "`), unless it starts with `#!&$>`. For multiple lines, starting with `#!&$<` in a line means there is a paragraph of content should be reserved, just like comments `/**/` in C or Java. You have to put that string at the head of line, and end it with `&!>` in anywhere. Noticed, the content after `&!>` will be discarded. Then the message will be stored and server returns a corresponding `ID`. At this time, server return status code `201` and render the web page according to the template. In default it's `createSucceed.html`. The placeholder `{{% code %}}` is the `ID`.

#### `/*` `GET`

This path only does one line work:

```java
ctx.redirect("https://github.com/hurui200320/Blake-Belladonna");
```

Guess what dose it do?

### Backend operation

#### `properties` file

There is a `properties` file store the settings of this program. Here are the explanations.

##### `ip`

The IP address the internal Jetty server(in Javalin) listening. Default is `0.0.0.0`

##### `port`

The port the internal Jetty server(in Javalin) listening. Default is `7000`

##### `data_directory`

The directory where the program store the messages file. In case there different path style in different OS, this parameter use ***ONLY*** relative path, relative with program. Default is `messages`

##### `message_theme`

The template using to render the message page. Default is `message.html`

##### `create_theme`

The template using to render the create message page. Default is `create.html`

##### `createSucceed`

The template using to render the message create succeed page. Default is `message.html`

##### `message_expired_time`

The time store on server in the unit of second. A message remain on server longer than this time will be automatic deleted. Set to `0` to ignore this. Default is `604800`

##### `max_messages`

The max amount of message stored on server. Reach this limit will result in status code `500` when creating new messages. Default is  `100000`

#### Regular check

The main function will register periodic task after starting the Javalin server. Task will be execute every hour:

```java
if(PropertiesUtils.getProperties().getMessageExpiredTime() != 0)
	Files.walkFileTree(PropertiesUtils.getProperties().getDataDirectory(), new SimpleFileVisitor<Path>(){
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Objects.requireNonNull(file);
			Messages messages;
			try{
				messages = Share.gson.fromJson(String.join("",Files.readAllLines(file)), Messages.class);
			}catch (Exception e){
				Files.delete(file);
				return FileVisitResult.TERMINATE;
			}

			if(messages.isExpired() && Files.exists(file))
				Files.delete(file);

			return super.visitFile(file, attrs);
		}
	});
```

It will read every message and check if it is expired. The message will be deleted if expired. Also it will call `gc()` at the end.

#### The store of messages(file)

The member variables in `Messages.class` are below:

```java
private String title = "", content = "";
private Timestamp sendTime = new Timestamp(System.currentTimeMillis());
```

And the member functions:

```java
public boolean isExpired(){
    if(PropertiesUtils.getProperties().getMessageExpiredTime() == 0)
		return false;
    if(System.currentTimeMillis() - this.sendTime.getTime() >= PropertiesUtils.getProperties().getMessageExpiredTime()*1000)
		return true;
	return false;
}

public String getTitle() {
	return new String(Base64.getDecoder().decode(title.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
}

public void setTitle(String title) {
	this.title = Base64.getEncoder().encodeToString(title.trim().getBytes(StandardCharsets.UTF_8));
}

public String getContent() {
	return new String(Base64.getDecoder().decode(content.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
}

public void setContent(String content) {
	this.content = Base64.getEncoder().encodeToString(content.trim().getBytes(StandardCharsets.UTF_8));
}

public Timestamp getSendTime() {
	return sendTime;
}

public String storeToFile(){
	...
}

public static Messages findMessageFile(String name) throws IOException {
    ...
}

public String storeToMysql(){
	//TODO
	return null;
}

public static Messages findMessageMysql(String name){
    // TODO
    return null;
}

@Override
public String toString() {
	...
}
```

The title and content will be apply `Base64` encode when setting them, which will prevent the content of them disturbing the `JSON` file. And `timestamp` is read-only.

The function `storeToFile()` is taking the responsibility of store the message:

```java
String name;
try {
	if (Files.notExists(PropertiesUtils.getProperties().getDataDirectory()))
		Files.createDirectories(PropertiesUtils.getProperties().getDataDirectory());

	CRC32 crc32 = new CRC32();
	crc32.update(json.getBytes(StandardCharsets.UTF_8));
	name = Long.toHexString(crc32.getValue()) + Long.toHexString(new Random().nextLong());

	if(Files.exists(Paths.get(PropertiesUtils.getProperties().getDataDirectory() + "/" + name.toUpperCase()))){
		Share.logger.error("File already exists: " + name.toUpperCase());
		return null;
	}

	Writer writer = Files.newBufferedWriter(Paths.get(
			PropertiesUtils.getProperties().getDataDirectory() + "/" + name.toUpperCase()));
	writer.write(json);
	writer.close();

}catch (IOException e){
	e.printStackTrace();
	Share.logger.error("Failed to store message: " + json);
	return null;
}
return name;
```

After preparing storing file, the message is first converted into `JSON` format. Then applied `CRC32` to it to generate the part of  `ID` . Another hex string of a random long value will be another part of `ID`, this may prevent duplicate `ID`s. When it really have a duplicate file name, the function will return `null` and cause the server return status code `500` to force user recreate the same message in different time. This will generate a new file name due to different timestamp and random value.

#### Find messages(file)

A static function placed in `Messages.class` will find the message. The function `findMessageFile（String name）` taking a string value which is the `ID`  and returning a `Messages` value, corresponding the message it found. If no message is found, it return null and cause the server return a status code `410`:

```java
public static Messages findMessageFile(String name) throws IOException {
	List<Messages> result = Collections.synchronizedList(new LinkedList<>());
	result.clear();
	Files.walkFileTree(PropertiesUtils.getProperties().getDataDirectory(), new SimpleFileVisitor<Path>(){
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Objects.requireNonNull(file);
			if(file.getFileName().toString().toUpperCase().equals(name.toUpperCase())){
				String read = String.join("",Files.readAllLines(file));
				Messages messages;
				try{
					messages = Share.gson.fromJson(read, Messages.class);
				}catch (Exception e){
					System.out.println(read);
					e.printStackTrace();
					messages = null;
				}finally {
					if(Files.exists(file))
						Files.delete(file);
				}
				if(messages != null)
					result.add(messages);
				return FileVisitResult.TERMINATE;
			}
			return super.visitFile(file, attrs);
		}
	});
	if(result.size() == 0)
		return null;
	return result.get(0);
}
```

## Copyright

Apache-2.0

