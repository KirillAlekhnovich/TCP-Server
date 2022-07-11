## Possible problems in OS Windows and Mac

In some Windows or Mac OS installations, there is a problem with standard configuration of virtual machine. If the tester in virtual machine cannot connect to the tested server in the host operating system, follow these steps:

* Additional step for Mac OS only, as Host-only Adapters in VirtualBox on do not work right out of the box: creating a Host-only Network in VirtualBox (File → Host Network Manager: "Create" button).
* When virtual machine with tester is off, change its network adapter settings from NAT to Host-only network (Host-only Adapter). In MacOS, select “vboxnet0” under the “name” drop down list.
* The network interface belonging to VirtualBox should appear in the host OS. This can be found from the command line with the ipconfig command. The IP address of this interface is likely to be 192.168.56.1/24.
* Now you need to manually set the IP address of eth0 network interface in the virtual machine with tester:

sudo ifconfig eth0 192.168.56.2 netmask 255.255.255.0

* Now you can start the tester but as the destination address enter the IP address of the network interface in the host OS:

tester 3999 192.168.56.1

* Do not forget to use that address in your server.