#!/bin/bash

# can only run as root
if [[ $EUID -ne 0 ]]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

MOUNT_DIR="storage"

# assuming:
# sudo apt-get install mdadm

# cleanup in case there's already a RAID...
# umount
umount /dev/md0
# detach existing loop devices
losetup -D
# remove existing RAID device
mdadm --remove /dev/md0


# generate a couple virtual disks
dd if=/dev/zero of=disk_0 bs=4096 count=50000
dd if=/dev/zero of=disk_1 bs=4096 count=50000
dd if=/dev/zero of=disk_2 bs=4096 count=50000

# initialize ext4 on them
mkfs -t ext4 disk_0
mkfs -t ext4 disk_1
mkfs -t ext4 disk_2

# setup disk files as devices
losetup /dev/loop0 disk_0
losetup /dev/loop1 disk_1
losetup /dev/loop2 disk_2

fsck /dev/loop0
fsck /dev/loop1
fsck /dev/loop2

# create the actual RAID device
mdadm --create /dev/md0 --level=5 --raid-devices=3 /dev/loop0 /dev/loop1 /dev/loop2

# format the RAID disk
# sudo fdisk /dev/md0
# g
# w

mkfs -t ext4 /dev/md0
fsck /dev/md0

mount /dev/md0 $MOUNT_DIR
