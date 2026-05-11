# backgammon_networks
## SERVER
Ubuntu
ps aux | grep java
java -jar backgammon-1.0-SNAPSHOT.jar server 5000
nohup java -jar backgammon-1.0-SNAPSHOT.jar server 5000 &



## CLİENT
cd C:\Users\merve\Documents\NetBeansProjects\backgammon2\backgammon_networks
java -jar target\backgammon-1.0-SNAPSHOT.jar


## GITHUB
https://github.com/mervekedersiz/backgammon_networks

## Sunucu baglanti karsilastirmasi

Bu projede istemci, AWS uzerindeki EC2 sunucusuna TCP socket ile baglanir. Sunucu tarafinda `BackgammonServer` varsayilan olarak `5000` portunu dinler. Istemci tarafinda ise `StartScreen` icinde varsayilan baglanti bilgisi su sekildedir:

```java
private static final String DEFAULT_HOST = "16.171.174.37";
private static final int DEFAULT_PORT = 5000;
```

### Hocanin kullandigi yontem

Hocanin orneginde baglanti icin EC2 public DNS adresi kullanilmis:

```java
new Socket("ec2-16-171-174-37.eu-north-1.compute.amazonaws.com", 5000);
```

### Benim yaptigim yontem

Benim kodumda ayni sunucuya public IPv4 adresi ile baglaniliyor:

```java
new Socket("16.171.174.37", 5000);
```

Public DNS adresi ile de ayni sunucuya baglanilabilir:

```java
new Socket("ec2-16-171-174-37.eu-north-1.compute.amazonaws.com", 5000);
```



