pragma solidity ^0.4.0;


contract AccountContract {

    mapping(uint256 => uint256) public publicKeyXById;
    mapping(uint256 => uint256) public publicKeyYById;

    function save(uint256 id, uint256 publicKeyX, uint256 publicKeyY) public {
        publicKeyXById[id] = publicKeyX;
        publicKeyYById[id] = publicKeyY;
    }

}
