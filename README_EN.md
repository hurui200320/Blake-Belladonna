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

程序首先寻找指定的消息，如果没有找到则直接返回状态码`410`。找到之后按照模板，默认情况下是与程序同目录下的`message.html`文件，文件输出时会将把标题占位符`{{% title %}}`替换成消息的标题，将内容占位符`{{% content %}}`替换为消息的实际内容。因此在二次开发或更换模板时应注意这些占位符，消息内容将被原封不动的插入到模板中，因此对于内容的预处理应当在创建消息时考虑。

#### `/new-message` `POST`

这个节点使用`POST`请求来处理创建消息的，通常由`/create`的表单发出请求。该节点接到请求时先检查已有消息的数量：

```java
File[] list = PropertiesUtils.getProperties().getDataDirectory().toFile().listFiles();
if(list != null && list.length >= PropertiesUtils.getProperties().getMaxMessages()){
    ctx.status(500);
    ctx.result("Too many message files in data directory!");
    Share.logger.error("Too many message files in data directory!");
    return;
}
```

如果已有消息数目达到了设定的数目，那么服务器返回状态码`500`，同时返回目录中保存的消息过多的提示信息。若没有超过数量限制，程序将检查`POST`请求所需要的必须字段：

```java
Map<String, List<String>> raw = ctx.formParamMap();
if(raw.keySet().containsAll(Arrays.asList(POST_TITLE_FIELD, POST_CONTENT_FIELD))){
	...
}else {
	ctx.status(400);
	ctx.result("bad request");
}
```

其中检查用的两个字段由常量定义：

```java
public static final String POST_TITLE_FIELD = "title";
public static final String POST_CONTENT_FIELD = "content";
```

如果请求中包含的键名不包括必需的字段，服务器将返回状态码`400`和对应的提示。如果有对应的字段则进行下一步处理：

```java
Messages messages = new Messages();
messages.setTitle(String.join("",raw.get(POST_TITLE_FIELD)));
String[] body = String.join("",raw.get(POST_CONTENT_FIELD)).split("\n");
StringBuilder sb = new StringBuilder();
for(String s : body){
	if(s.startsWith("#!&$>") && s.length() > 5){
		sb.append(s.substring(5));
	}else {
		sb.append("<p>" + s + "</p>\n");
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

消息标题保留原样，针对每一行内容都翻译成`HTML`的段落。如果以特定的`#!&$>`开头，则不进行翻译。完成后对消息进行储存，储存成功后将得到一个对应的`ID`，此时服务器返回状态码`201`，同时引用模板将得到的`ID`显示给用户。默认情况下模板是与程序同目录下的`createSucceed.html`，其中的占位符`{{% code %}}`替换成得到的`ID`。

#### `/*` `GET`

这个节点的代码就一行：

```java
ctx.redirect("https://github.com/hurui200320/Blake-Belladonna");
```

你猜猜他是干啥的？

### 后端操作

#### `properties`文件

程序默认使用Java提供的`properties`文件存储设置。当前版本的设置次列如下：

##### `ip`

这个字段是设置Javalin内置服务器Jetty所监听的IP地址。默认值：`0.0.0.0`

##### `port`

这个字段是设置Javalin内置服务器Jetty所监听的端口号。默认值：`7000`

##### `data_directory`

该字段设置程序储存消息所用的文件夹路径。为了避免不同系统下文件分隔符的不兼容，这里强制使用相对路径下的名称，文件夹将与程序处在同一目录下。默认值：`messages`

##### `message_theme`

该字段储存程序渲染显示消息页面所用的模板。为了避免不同系统下文件分隔符的不兼容，这里强制使用相对路径下的名称，模板需与程序处在同一目录下。默认值：`message.html`

##### `create_theme`

该字段储存程序渲染显示消息页面所用的模板。为了避免不同系统下文件分隔符的不兼容，这里强制使用相对路径下的名称，模板需与程序处在同一目录下。默认值：`create.html`

##### `createSucceed`

该字段储存程序渲染显示消息页面所用的模板。为了避免不同系统下文件分隔符的不兼容，这里强制使用相对路径下的名称，模板需与程序处在同一目录下。默认值：`message.html`

##### `message_expired_time`

该字段保存消息的有效期，单位是秒。过期的消息将会被定期删除。设置位`0`时忽略有效期，消息一直有效。默认值：`604800`

##### `max_messages`

该字段保存最大允许储存的消息数目，即储存消息目录下最大的文件数目。当消息数量超过此数量时，创建消息的操作将被返回状态码`500`。默认值`100000`

#### 定时检查

主函数在启动Javalin服务器之后还注册了定时任务。当前版本的设定是每小时执行一次。定时任务内容如下：

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

定时任务执行时将会读取每个文件并检查其是否过期。过期则删除文件。

同时每次定期任务执行结束时将调用`gc()`。

#### 消息的储存（文件）

在当前版本中消息的实现中有如下成员变量：

```java
private String title = "", content = "";
private Timestamp sendTime = new Timestamp(System.currentTimeMillis());
```

同时还有成员函数：

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

其中对于标题和内容这两个字段，在储存时使用了`Base64`进行编码，用以防止用户输入的内容对保存文件所使用的`JSON`格式有所干扰。另外时间戳字段只能读取不能进行设定。

在消息的储存方面，即`storeToFile()`函数，其实现如下：

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

在做好写入文件的准备操作后，首先将要储存的消息转换成`JSON`格式，此处使用了Google的`Gson`。随后对该字符串进行`CRC32`校验，产生长度为8的Hex字符串。随后该字符串与一个值随机长整型数的字符串组合，在一定限度上确保消息保存的文件名不会重复。若遇到文件名重复则返回`null`，这将导致服务器对创建消息的请求回应状态码`500`。这样可使用户尝试重新创建消息，在不同的时间下可以通过时间戳和随机数的变化改变文件名。

#### 消息的查找（文件）

查找文件的操作作为同步静态方法放在了`Messages`类中。函数`findMessageFile（String name）`接收一个字符串变量，返回`Messages`类型的返回值。若消息存在则返回对应的对象，若不存在则返回`null`，这样导致服务器对查询消息的请求返回状态码`410`。函数实现如下：

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

由于重复名字的消息要么没有，要么就只有一个，因此找到一个后返回即可。若未找到则返回`null`。

## 版权

本项目以Apache 2.0许可授权。

