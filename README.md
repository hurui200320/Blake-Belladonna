# Blake-Belladonna

基于Java和Javalin的开源阅后即焚服务。

**English version is [here](./README_EN.md).**

[TOC]

## 简介

这是一个基于Java和Javalin框架实现的网页版阅后即焚服务。你可以通过它来一次性地传达一个消息。首先你要将消息保存在服务器上，服务器会返回给你你个链接，随后便可以将链接分享给别人了。当其他人一旦访问了链接，服务器处理到了请求，你的消息将在渲染成网页形式显示给访问者之后立刻从服务器上移除，随即将不可能再次被显示。

你可以点[击此处来创建一条阅后即焚消息](https://blake.skyblond.info/create)。

## 详细的使用说明和简要原理

### 创建消息

在创建消息时可以输入任意内容。但网站中分别有中英文的提醒，此处再次重申：**你正在编辑的信息将被储存到服务器上，并且没有经过加密。这意味着网站的主人可以看到你输入的内容。因此请不要在此处输入任何隐私信息。除非确保它已经被妥当加密，并且你愿意承担这些信息被泄露的后果。否则请不要在此处输入任何不宜公开的信息。**如果一定要加密，请务必使用安全的加密算法，例如AES等，并且强烈**不建议**将密文与密钥**同时**通过这个阅后即焚服务进行分享，请考虑使用其他安全的信道（如短信，电话等方式）。

### 分享消息

如果消息提交成功，你将会在页面上看到一个链接，复制并分享即可。请注意，这个链接无预览功能，一经访问即刻失效。但是同一条（指内容和标题同时相同）消息可以重复提交多次，链接不会重复。**但请注意：超过7天仍未被阅读的消息将会被删除，同时链接将失效。另外链接遗失将无法找回。**

### 查看消息

通过在浏览器中输入链接并访问，消息会被渲染成网页，网页中有红字提醒这是一条阅后即焚消息，无法在关闭或刷新网页后再现消息。如果访问的消息不存在或者已经被删除，服务器将给出410响应（服务器并不区分消息不存在`404`和消息已经被删除`410`，而是统一以`410`响应）。

### 进阶输入

此段主要讲解如何使用`HTML`语言使得消息内容更加丰富。

关于消息内容本身并无限制。输入的文本将按照换行自动翻译成`HTML`的段落。即`<p>输入的一行文字</p>`。如果你需要输入的内容不被翻译成段落，按照程序设定，请在**每一行**的开头顶头（不允许前面有空格）加入：`#!&$>`这五个字符。程序在处理时将会把这五个字符去掉，余下的内容原封不动的存入消息。**但请注意：不完整或有问题的html代码将会影响显示消息内容网页的正确渲染，可能会导致你的消息无法被正常显示。**有关消息显示页面的更多信息，请见下面的[面向开发者的详细原理解释](#面向开发者的详细原理解释)。

## 面向开发者的详细原理解释

### 网页交互

[Javalin](https://javalin.io/)负责web方面的交互，程序一共使用了4个节点。之后将以`<节点路径> <请求方式>`的格式一一介绍。程序本身不关心网页上的交互内容，只进行请求的处理和后端的处理操作。消息的创建、创建成功反馈和显示所用到的网页均是按照我最初的Email信纸魔改而来的。关于后者的来源与借鉴之处，以及最原始代码的版权请移步[博文：「【歪门邪道】定制 Email 样式 及 Dreamweaver 试用」](https://www.skyblond.info/archives/2018/10/27/529)。

#### `/create` `GET`

这个节点负责显示创建消息相关的网页。内容处理上也仅有两行代码：

```java
ctx.header("content-type","text/html; charset=UTF-8");
ctx.result(String.join("\n", Files.readAllLines(PropertiesUtils.getProperties().getCreateTheme())));
```

默认情况下显示的是与源程序同目录下的`create.html`。在二次开发或更换模板时应该留意表单的`action`要对应以`POST`请求处理创建消息的节点`new-message`，同时还应该保证标题与内容的数据键名能够对应`Main.class`中的`POST_TITLE_FIELD`字段和`POST_CONTENT_FIELD`。目前版本中他们的值分别为`title`和`content`。

#### `/show/:checksum` `GET`

这个节点负责消息的显示，也是最先被实现的一个接口。在实际使用中，`:checksum`将会被替换成消息对应的`ID`进而组成一个完整的URL。程序会读取`:checksum`处的值从而寻找对应的消息。此处的代码实现如下：

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

其中有一个`if(true)`，此处是一个遗留代码，从注释中可以看出原本还在设置中设计的储存模式，`0`代表以文件储存，其他值表示以MySQL存储，但是后来发现以文件储存的效率感觉还行，所以MySQL那个实现就先放下了，打算之后再说。

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

消息标题保留原样，针对每一行内容都翻译成`HTML`的段落并进行转义。如果以特定的`#!&$>`开头，则不进行翻译。如果一行以`#!&$<`开头，则表示一段内容不进行翻译，则类似C语言中`/**/`这样的段落注释，但要求上述字符串前不能有多于字符。段落保留以`&!>`结束，该字符串可在行内的任意位置出现，一行内其后的内容将被丢弃。上述操作完成后对消息进行储存，储存成功后将得到一个对应的`ID`，此时服务器返回状态码`201`，同时引用模板将得到的`ID`显示给用户。默认情况下模板是与程序同目录下的`createSucceed.html`，其中的占位符`{{% code %}}`替换成得到的`ID`。

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

查找文件的操作作为静态方法放在了`Messages`类中。函数`findMessageFile（String name）`接收一个字符串变量，返回`Messages`类型的返回值。若消息存在则返回对应的对象，若不存在则返回`null`，这样导致服务器对查询消息的请求返回状态码`410`。函数实现如下：

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

