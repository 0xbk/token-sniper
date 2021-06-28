import "./IERC20.sol";

abstract contract PrintrToken is IERC20  {
    uint256 public balanceLimit;
    uint256 public sellLimit;
    bool public tradingEnabled;
    bool public whiteListTrading;
    function getSellLockTimeInSeconds() public virtual view returns(uint256);
}
